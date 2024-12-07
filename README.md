# NotificationSync
Forward your notifications from one Android device to another via FCM

You can use an huawei or xiaomi or oppo or vivo or meizu device as a server, 
to forward the notifications pushed by hmsPush or miPush or oppoPush or vivoPush or meizuPush to your main daily device like
samsung galaxy, google pixel, sony xperia etc.
For the reason that most of chinese android app do not support google fcm push, and the device besides huawei xiaomi oppo vivo and
meizu can only use google fcm to receive background notifications(fuck gfw)
your main device don't need to be rooted, and the server also, only you need for the server is smooth international Internet.
and your main device can receive fcm push.

and also the samsung china has cooperated with Getui and Jiguang Push, to provide unified push service, but only few 
chinese apps use it to push ads, not even important information.

![WelcomeScreen](https://github.com/HappyMax0/NotificationSync/blob/main/demoImages/Screenshot_20241205_213310_NotificationSync.jpg)

![ServerScreen](https://github.com/HappyMax0/NotificationSync/blob/main/demoImages/Screenshot_20241205_213244_NotificationSync.jpg)

![AppListScreen](https://github.com/HappyMax0/NotificationSync/blob/main/demoImages/Screenshot_20241205_213259_NotificationSync.jpg)


您可以使用一个华为或小米或oppo或vivo或魅族设备作为服务器，
将hmsPush或miPush或oppoPush或vivoPush或meizuPush的通知转发到你的主要日常设备，如三星galaxy、google pixel、索尼xperia等。
由于大多数中国安卓应用不支持google fcm推送，除华为、小米、oppo、vivo和魅族之外的设备只能使用google fcm接收后台通知（去他妈的gfw）
你的主设备不需要root，服务器也一样，你只需要服务器有顺畅的国际互联网。
你的主设备可以接收fcm推送。

此外，三星中国已经与个推和极光推送合作，提供统一的推送服务，但只有少数中国应用使用它来推送广告，甚至不是重要的信息。

统一推送联盟 UPA 的使命已经完成了官网域名都在出售了
统一推送服务 UPS 是一套规范 ,服务器还是各自厂商自己维护的 ,并不是工信部管理推送服务器 。所以 大部分厂商都是在各自原有推送服务的基础上支持了 UPS 规范 。
下列厂商说是已经支持了 UPS 规范
• 华为统一推送 : 华为
• 荣耀统一推送 : 荣耀
• OPPO 统一推送 : OPPO （需要 ColorOS 3.0 以上） 、realme" （全部） 、一加 （一加 5 及以上）
• 中兴统一推送 ：中兴
• 极光 UPS 三星（基本没有 app 接入，什么支付宝，抖音，小红书都没有，只有淘宝会推送统一广告，不会推送正经消息，还有三星商城会拿来推送广告）
• 个推 UPS：索尼（基本没有 app 接入）
• VIV0 统一推送 : VIVO 、iQ00
• 小米统一推送 : 小米、红米
虽然这些厂商支持了 UPS 但是每个 app 还要单独接入这些厂商才行 。 并不是厂商说支持了 手机就能收到推送了。微信这种流氓肯定不会接入系统级 推送的 。 第一是因为系统级推送通知不能被 app 完全控制 。第二是到达率和到达及时性问题 。不过即使微信不接入系统级推送： 手机厂商仍然会将它加入不杀后台的白名单 ,否则 会被用户认为这手机垃圾 。
