# GPS路径追踪Demo

使用百度和高德的定位SDK获取GPS位置，并利用地图SDK进行路径显示。

* bdMap module 是百度地图SDK的demo应用。
* aMap module 是高德地图的demo应用。

## 百度地图-离线地图功能

离线地图必须通过百度SDK提供的下载API从App直接下载，但为了测试方便，我将北京市的离线地图数据从手机中pull出来了，可以直接[下载][bj-download]使用。

下载后，解压得到“BaiduMapSDKNew”目录。push到 `/sdcard/Android/data/<应用包名>/files/BaiduMapSDKNew` 目录中。

之后打开应用，即可使用北京市的离线地图了。

获取更多详情，请看[百度官方文档][baidu-offline]。

[baidu-offline]: http://wuxian.baidu.com/map/map.html
[bj-download]: /assets/BaiduSDK_Beijng_131.zip

