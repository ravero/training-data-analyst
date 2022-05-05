/*
 * Copyright 2018 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.training.appdev.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiService;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.MessageReceiver;
import com.google.cloud.pubsub.v1.Subscriber;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.PushConfig;
import com.google.pubsub.v1.SubscriptionName;
import com.google.pubsub.v1.TopicName;


import com.google.training.appdev.services.gcp.domain.Feedback;
import com.google.training.appdev.services.gcp.languageapi.LanguageService;
import com.google.training.appdev.services.gcp.spanner.SpannerService;

public class ConsoleApp {

    public static void main(String... args) throws Exception {
        String projectId = System.getenv("GCLOUD_PROJECT");
        System.out.println("Project: " + projectId);

        // Notice that the code to create the topic is the same as in the publisher
        TopicName topic = TopicName.create(projectId, "feedback");
        LanguageService languageService = LanguageService.create();
        SpannerService spannerService = SpannerService.create();
        SubscriptionName subscription = SubscriptionName.create(projectId, "worker1-subscription");

        try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create()) {
            subscriptionAdminClient.createSubscription(subscription, topic, PushConfig.getDefaultInstance(), 0);
        }

        // The message receiver processes Pub/Sub subscription messages
        MessageReceiver receiver = new MessageReceiver() {
            // Override the receiveMessage(...) method
            @Override
            public void receiveMessage(PubsubMessage message, AckReplyConsumer consumer) {
                String fb = message.getData().toStringUtf8();
                consumer.ack();

                try {
                    // Object mapper deserializes the JSON String
                    ObjectMapper mapper = new ObjectMapper();

                    Feedback feedback = mapper.readValue(fb, Feedback.class);
                    System.out.println("Feedback received: " + feedback);

                    float sentimentScore = languageService.analyzeSentiment(feedback.getFeedback());
                    feedback.setSentimentScore(sentimentScore);
                    System.out.println("Score is: " + sentimentScore);

                    // TODO: Insert the feedback into Cloud Spanner
                    spannerService.insertFeedback(feedback);
                    System.out.println("Feedback saved");

                    // END TODO

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        Subscriber subscriber = null;


        try {
            subscriber = Subscriber.defaultBuilder(subscription, receiver).build();
            subscriber.addListener(
                    new Subscriber.Listener() {
                        @Override
                        public void failed(ApiService.State from, Throwable failure) {
                            System.err.println(failure);
                        }
                    },
                    MoreExecutors.directExecutor()
            );

            subscriber.startAsync().awaitRunning();
            System.out.println("Started. Press any key to quit and remove subscription");
            System.in.read();

        } finally {
            if (subscriber != null) {
                subscriber.stopAsync().awaitTerminated();
            }
            try (SubscriptionAdminClient subscriptionAdminClient = SubscriptionAdminClient.create()) {
                subscriptionAdminClient.deleteSubscription(subscription);
            }

        }
    }

}

