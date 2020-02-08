# required by Android-Iconics;
# it will take effect when code shrinking, obfuscation, and optimisation are enabled (set "minifyEnabled" to true; some other rules are also required)
-keep class .R
-keep class **.R$* {
    <fields>;
}