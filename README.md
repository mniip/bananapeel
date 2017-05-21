# BananaPeel
BananaPeel is the codename for an Android IRC client.

## Planned features
- IRCv3 support.
- Heavy load support with low resource footprint.
- Client-side scripting.
- Strong security.
- Support for android all the way back to 2.3 (API 9).

You can track the  feature support on the [TODOs page](https://github.com/mniip/bananapeel/wiki/TODOs).

## Dependencies
- Android support library: v4 and recyclerview-v7
- CGLIB
- LuaJ

## Building with make
You should point the `SDK_PATH` environment variable at the Android SDK root directory, and either have the Android build-tools in your path, or point `SDK_BUILD_TOOLS` at the directory where they are located.
Paths to dependencies should be put into `LIB_JARS` and `LIB_AARS` variables respectively, as space-separated lists.

Once that is done, simply run:

    make

## Building with Gradle/Android Studio
You can build the project using the provided Gradle files, or import the project as a Gradle project (via `settings.gradle`) in Android Studio.
