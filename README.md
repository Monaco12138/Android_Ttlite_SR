# Android_Tflite_SR
这是一个完整的部署超分辨率模型到安卓设备的解决方案，包含了模型的训练，量化导出，测试，以及部署。

本仓库包含模型的部署部分，使用Android studio将经过训练导出好的tflite模型部署到Android手机上，具体的如何训练，量化导出，测试模型见[另一仓库](https://github.com/Monaco12138/SR_Tensorflow)

## 环境配置
下载Android studio，clone本仓库用Android studio打开，确保./app/build.gradle.kts 相关依赖库能正常下载，重新Rebuild Project，连接自己手机编译运行安装即可

