#!/bin/bash
# This script is used to setup the environment for the Java Microservices Labs

rm -rf ~/guestbook-service
rm -rf ~/guestbook-frontend

cp -a ~/spring-cloud-gcp-guestbook/1-bootstrap/guestbook-service ~/guestbook-service
cp -a ~/spring-cloud-gcp-guestbook/1-bootstrap/guestbook-frontend ~/guestbook-frontend
