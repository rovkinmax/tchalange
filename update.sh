#!/bin/bash

function bumpVersion(){
	perl -pe 's/(versionCode )(\d+)/$1.($2+1)/e' < app/build.gradle > tmp
	perl -pe 's/(versionName \"\d+\.\d+\.)(\d+)(\")/$1.($2+1).$3/e' < tmp > app/build.gradle
	rm tmp
}



bumpVersion

./gradlew publishRelease --stacktrace|| exit

bumpVersion # again
#git
git add .
git ci -m "bump version"
git push





