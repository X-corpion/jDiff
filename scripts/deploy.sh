#!/usr/bin/env bash
set -exo pipefail
if [[ ${TRAVIS_BRANCH} = 'master' ]] && [[ ${TRAVIS_PULL_REQUEST} = 'false' ]]; then
    openssl aes-256-cbc -K ${encrypted_39dc14e92501_key} -iv ${encrypted_39dc14e92501_iv} -in signing.asc.enc -d | ${GPG_EXECUTABLE} --fast-import
    if [[ ${TRAVIS_TAG} != rel/* ]]; then
        echo 'No Tag Defined. Deploying Snapshot.'
        mvn build-helper:parse-version versions:set -DnewVersion='${parsedVersion.majorVersion}.${parsedVersion.minorVersion}.${parsedVersion.incrementalVersion}-SNAPSHOT' versions:commit
    fi
    mvn -B -U -P ossrh -DskipTests=true --settings .travis-settings.xml deploy
fi