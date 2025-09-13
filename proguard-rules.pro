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