#!/usr/bin/env bash

export LAUNCHER="io.vertx.core.Launcher"
export VERTICLE="iudx.catalogue.server.deploy.ReDeployerDev"
export CMD="mvn compile"
export VERTX_CMD="run"

mvn compile dependency:copy-dependencies
java \
  -cp  $(echo target/dependency/*.jar | tr ' ' ':'):"target/classes" \
  $LAUNCHER $VERTX_CMD $VERTICLE -conf "configs/config-depl.json" \
  --redeploy="src/main/**/*" --on-redeploy="$CMD" \
  --launcher-class=$LAUNCHER \
  $@
