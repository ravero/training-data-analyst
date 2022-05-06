#!/bin/bash
# This script is used to setup the environment for the Java Microservices Labs

## Set the branch
git fetch --all
git switch $1

## Create the Linked Folders
ln -s ~/training-data-analyst/courses/java-microservices/spring-cloud-gcp-guestbook ~/spring-cloud-gcp-guestbook

