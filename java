#!/bin/bash

set -e -o pipefail

error() {
	echo -e "\e[1m$@\e[0m" > /dev/stderr
	false
}

nativeDir=
classPath=
cpArg=

args=("$@")

for i in $(seq 0 $((${#args}-1))); do
	if [[ "${args[$i]}" == "-Djava.library.path="* ]]; then
		nativeDir=$(echo "${args[i]}" | cut -d= -f2)
	elif [[ "${args[$i]}" == "-cp" ]]; then
		cpArg=$(($i+1))
		IFS=$'\n' classPath=($(echo ${args[$cpArg]} | tr : $'\n'))
	fi
done

if [[ $nativeDir == "" || $cpArg == "" ]]; then
	exec java "$@"
fi

mmcdir="$(echo "$nativeDir" | sed -E 's@/instances/.*/natives$@@')"
IFS=$'\n' lwjglLibs=($(printf '%s\n' "${classPath[@]}" | grep lwjgl))
IFS=$'\n' otherLibs=($(printf '%s\n' "${classPath[@]}" | grep -v lwjgl))
jars=($(for lib in "${lwjglLibs[@]}"; do echo "$lib" | sed -nE 's/.*\/(lwjgl.*)-.*.jar/\1/p'; done))
lwjglVersion=$(basename ${lwjglLibs[0]} | sed -nE 's/.*-([0-9\.]*)\.jar/\1/p')

archsuffix=

case $(uname -m) in
	x86_64)
		;;
	aarch64)
		archsuffix=-arm64
		;;
	armhf)
		archsuffix=-arm32
		;;
	*)
		echo "This architecture is not supported."
		exit 1
		;;
esac

if [[ $(echo $lwjglVersion | sed 's/\.//g') < 323 ]]; then
	lwjglVersion=3.2.3
fi

mavenbase="https://repo1.maven.org/maven2"

jarurl() {
	echo "$mavenbase/org/lwjgl/$1/$lwjglVersion/$1-$lwjglVersion.jar"
}

nativeurl() {
	echo "$mavenbase/org/lwjgl/$1/$lwjglVersion/$1-$lwjglVersion-natives-linux$archsuffix.jar"
}

destDir="${mmcdir}/arm-jars/$lwjglVersion"
mkdir -p "$destDir"

for file in "${jars[@]}"; do
	jar="$(jarurl $file)"
	natives="$(nativeurl $file)"
	destJar="$destDir/$(basename $jar)"
	destNatives="$destDir/$(basename $natives)"

	if [[ ! -f "$destJar" ]]; then
		curl -o "$destJar" "$jar" || error "Failed to download jar!"
	fi
	if [[ ! -f "$destNatives" ]]; then
		curl -o "$destNatives" "$natives" || error "Failed to download natives jar!"
	fi
done

args[$cpArg]="$(cat <(find $destDir -type f) <(printf '%s\n' "${otherLibs[@]}") | tr $'\n' :)"

echo "${args[@]}"
exec java "${args[@]}"
