#!/usr/bin/env bash
set -e

arg_len=$#
all_args="$@"
args=""

skip_tools=n

remove_shadow="-x shadowJar"
shadow=${shadow:-$remove_shadow}
main_args="-Dxapi.composite=true -Pxapi.changing=true --parallel --build-cache -Pxapi.debug=false $shadow"

while (( arg_len > 0 )); do
  arg_ind=$(( 1 + $# - arg_len ))
  arg="${!arg_ind}"
  case "$arg" in
    --forcePublish|-fP)
      echo "Forcing publishing"
      all_args="${all_args/$arg}"
      main_args="$main_args -PxapiForcePublish=true"
      ;;
   --shadow|-s)
      echo "Allowing shadow jar"
      all_args="${all_args/$arg}"
      main_args="${main_args/$remove_shadow}"
      ;;
   --main|-m)
      if (( arg_ind == $# )); then
          echo "Must provide an argument after --main"
          exit 123
      fi
      next_arg=$(( arg_ind + 1 ))
      to_main="${!next_arg}"
      echo "Adding $to_main to main_args"
      main_args="$main_args $to_main"
      arg_len=$(( arg_len - 1 ))
      ;;
   --no-tool|-nT)
      echo "Skipping tool build"
      skip_tools=y
      ;;
   --java11|--jdk11|-j11)
      echo "Java11 requested, skipping select incompatible groovy tasks"
      main_args="$main_args -x :xapi-lang-test:compileTestGroovy -x :xapi-lang-test:compileGroovy"
      # Hm... should really make this something portable
      if [ -d /usr/lib/jvm/adoptopenjdk-11-hotspot-amd64 ]; then
          export JAVA_HOME=/usr/lib/jvm/adoptopenjdk-11-hotspot-amd64
          export PATH="$JAVA_HOME/bin:$(echo "$PATH" | sed -e "s/[^:]*adoptopenjdk[^:]*://g")"
      fi
      ;;
   *)
      echo "Found pass-thru argument $arg"
      args="$args $arg"
      # Some special flags will cause us to erase some main_args
      [[ "--no-build-cache" == "$arg" ]] && main_args="${main_args/ --build-cache}" || true
      [[ "-Dxapi.composite=false" == "$arg" ]] && main_args="${main_args/-Dxapi.composite=true/-Dxapi.composite=false}" || true
      
      ;;
  esac
      
  arg_len=$(( arg_len - 1 ))
done

# Check if there are any more user-supplied arguments that we didn't handle, and insert default task list:
function has_args() {
    if [[ -z "$args" ]]; then
        return 1
    fi
    for arg in $args; do
        echo "Checking $arg"
        case "$arg" in
            -*)
            ;;
            *)
            return 1
        esac
    done
    return 0
}
has_args || args="build xapiPublish testClasses -x test -x check"

echo "Running all builds' gradlew $args"
echo "Running main build w/ arguments: $main_args $args"

function do_it() {
    if [ "$skip_tools" == n ]; then
        pushd net.wti.gradle.tools > /dev/null
        # the tools will install themselves to local repo whenever we build them.
        echo "invoking ./gradlew $args in $(pwd)"
        ./gradlew $args
        popd > /dev/null


        pushd net.wti.core > /dev/null
        echo "invoking ./gradlew $args in $(pwd)"
        ./gradlew $args
        popd > /dev/null


        pushd net.wti.gradle > /dev/null
        echo "invoking ./gradlew $args in $(pwd)"
        ./gradlew $args
        popd > /dev/null
    fi

    ./gradlew $main_args $args

}
TIMEFORMAT='Full build time: %3Rs'
time do_it
