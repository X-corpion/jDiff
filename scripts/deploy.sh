#!/usr/bin/env bash
set -exo pipefail
if [[ ${TRAVIS_BRANCH} = release/* ]] && [[ ${TRAVIS_PULL_REQUEST} = 'false' ]]; then
    openssl aes-256-cbc -K ${encrypted_39dc14e92501_key} -iv ${encrypted_39dc14e92501_iv} -in signing.asc.enc -d | ${GPG_EXECUTABLE} --fast-import
    mvn -B -U -P ossrh -DskipTests=true --settings .travis-settings.xml deploy gitflow:release-finish
fi