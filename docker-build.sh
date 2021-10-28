#!/bin/bash

# This script can be used to build the Docker image locally, since `docker-compose build` does not support Buildkit secrets

settings=${MLS_M2_SETTINGS:-~/.m2/settings.xml}

for type in gmaps poi vienna; do
  module=geocoding-${type}
  app=geocoding-${type}
  tag=wrkfmdit/geocoding-${type}:${MLS_TAG:-latest}

  echo "Running Docker build for ${app} with secrets from '${settings}'..."

  DOCKER_BUILDKIT=1 docker build \
      --secret id=m2-settings,src="${settings}" \
      --build-arg MODULE=$module --build-arg APP=$app \
      -t "$tag" .
done
