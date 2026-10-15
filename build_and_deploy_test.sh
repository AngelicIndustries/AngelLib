VERSION=testerino ./gradlew build
mkdir -p ../AuthTests/run/libs
mkdir -p ../WawelAuth/run/libs
cp build/libs/angellib-testerino-dev.jar ../AuthTests/run/libs/angellib-testerino-dev.jar
cp build/libs/angellib-testerino-dev.jar ../WawelAuth/run/libs/angellib-testerino-dev.jar
