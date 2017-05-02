#!/usr/bin/env bash
set -euo pipefail

SECO_HFST_VERSION="1.1.5"
SECO_LAS_VERSION="1.5.4"
TRANSDUCER_VERSION="1.4.9"

my_dir=$(pwd)

if [ ! -f ~/.m2/repository/fi/seco/lexicalanalysis/${SECO_LAS_VERSION}/lexicalanalysis-${SECO_LAS_VERSION}.jar ]; then
  echo "Install SeCo dependencies..."
  # ensure fresh copy
  mkdir -p target/deps
  rm -rf target/deps/*
  cd target/deps

  # transitive dependencies
  mkdir lib
  curl -L -o lib/anna-3.6.jar https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/mate-tools/anna-3.6.jar
  curl -L -o lib/marmot-2014-10-22.jar https://github.com/TurkuNLP/Finnish-dep-parser/raw/master/LIBS-LOCAL/marmot/marmot-2014-10-22.jar

  mvn install:install-file \
         -Dfile=lib/anna-3.6.jar \
         -DgroupId=is2 \
         -DartifactId=anna \
         -Dversion=3.6 \
         -Dpackaging=jar \
         -DgeneratePom=true

  mvn install:install-file \
         -Dfile=lib/marmot-2014-10-22.jar \
         -DgroupId=marmot \
         -DartifactId=marmot \
         -Dversion=2014-10-22 \
         -Dpackaging=jar \
         -DgeneratePom=true

  # actual dependencies
  sh -c "git clone https://github.com/jiemakel/seco-hfst.git hfst && cd hfst && git checkout v${SECO_HFST_VERSION}"
  sh -c "git clone https://github.com/jiemakel/seco-lexicalanalysis.git las && cd las && git checkout v${SECO_LAS_VERSION}"
  curl -L -o las/models.tar.xz https://github.com/jiemakel/seco-lexicalanalysis/releases/download/v${TRANSDUCER_VERSION}/transducers-and-models.tar.xz
  sh -c "cd las && tar vxf models.tar.xz"
  sh -c "cd hfst && mvn install -Dgpg.skip"
  sh -c "cd las && mvn install -Dgpg.skip -Dproject.build.sourceEncoding=UTF-8 -DskipTests"

  # cleanup
  cd ${my_dir} && rm -rf target/deps
else
  echo "SeCo dependencies already installed"
fi

echo "Build las-emr..."
./lein do clean, compile, uberjar

echo "All done!"
