package me.noramibu.mixin;

import me.noramibu.data.HappyGhastData;

/**
 * 快乐恶魂数据访问器接口
 * 用于在Mixin中存储和访问快乐恶魂的自定义数据
 */
public interface HappyGhastDataAccessor {
    /**
     * 获取快乐恶魂数据
     * @return 快乐恶魂数据对象
     */
    HappyGhastData getGhastData();
    
    /**
     * 设置快乐恶魂数据
     * @param data 快乐恶魂数据对象
     */
    void setGhastData(HappyGhastData data);
}
