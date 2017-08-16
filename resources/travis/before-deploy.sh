#!/bin/bash
echo 'Starting before-deploy.sh'
if [ "$TRAVIS_BRANCH" = 'master' ] || [ "$TRAVIS_BRANCH" = 'develop' ]; then
    if [ "$TRAVIS_PULL_REQUEST" == 'false' ]; then
        echo "Decrypting code signing key"
        openssl aes-256-cbc -K $encrypted_602ef6a355db_key -iv $encrypted_602ef6a355db_iv -in resources/travis/codesignkey.asc.enc -out codesignkey.asc -d 
        gpg --fast-import codesignkey.asc
    fi
fi

echo 'Completed before-deploy.sh'
