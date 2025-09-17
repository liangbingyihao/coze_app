-keep class com.bytedance.speech.speechengine.SpeechEngineImpl {*;}
# 保留数据模型类
-keep class sdk.chat.demo.robot.api.model.** { *; }
-keep class sdk.chat.demo.robot.adpter.data.** { *; }

# 保留注解
-keepattributes *Annotation*

# 保留 native 方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# ProGuard rules for tinylog

-keepnames interface org.tinylog.**
-keepnames class * implements org.tinylog.**
-keepclassmembers class * implements org.tinylog.** { <init>(...); }

-dontwarn dalvik.system.VMStack
-dontwarn java.lang.**
-dontwarn javax.naming.**
-dontwarn sun.reflect.Reflection