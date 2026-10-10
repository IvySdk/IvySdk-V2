package com.ivy.base.helper;

public interface IHelper {

    boolean isHelperInitialized();

    boolean hasNewHelperMessage();

    /**
     * @param entranceId     自定义入口id
     * @param meta           JsonObject格式，用户数据
     * @param tags           用户标签；在具体为用户配置标签时，你应该确保已经提前在 AIHelp 后台配置好了对应的标签内容
     * @param welcomeMessage 自定义欢迎语
     */
    void showHelper(String entranceId, String meta, String tags, String welcomeMessage);

    /**
     * 打开指定 faq 页面
     *
     * @param faqId
     * @param moment FAQ 页面展示联系客服按钮的时机: 0 不显示；1 点踩后显示；2 只在回复页显示； 默认一直显示；
     */
    void showHelperSingleFAQ(String faqId, int moment);

    /**
     * 获取未读消息数,AIHelp每5分钟轮询一次
     *
     * @param onlyOnce 是否只获取一次
     */
    void listenHelperUnreadMessageCount(boolean onlyOnce);

    void stopListenHelperUnreadMessageCount();

    /**
     * 配置用户信息，携带用户标签
     * 在具体为用户配置标签时，你应该确保已经提前在 AIHelp 后台配置好了对应的标签内容
     *
     * @param data
     * @param tags
     */
    void updateHelperUserInfo(String data, String tags);

    void updateHelperLanguage(String l);

    /**
     * 重置用户信息
     */
    void resetHelperUserInfo();

    void closeHelper();
}

