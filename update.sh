#!/bin/bash

#increase verion
perl -pe 's/(versionCode )(\d+)/$1.($2+1)/e' < main_lib/build.gradle > tmp
perl -pe 's/(versionName \"\d+\.\d+\.)(\d+)(\")/$1.($2+1).$3/e' < tmp > main_lib/build.gradle
rm tmp
#git
git add .
git ci -m "bump version"
#publish
./gradlew publishRelease




