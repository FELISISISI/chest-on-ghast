package me.noramibu.accessor;

import me.noramibu.data.HappyGhastData;

/**
 * 快乐恶魂数据访问器接口
 * 用于在Mixin中存储和访问快乐恶魂的自定义数据
 * 
 * 注意：此接口必须放在非mixin包中，以便可以被普通代码引用
 */
public interface HappyGhastDataAccessor {
    /**
     * 获取快乐恶魂数据
     * @return 快乐恶魂数据对象
     */
    HappyGhastData getHappyGhastData();
    
    /**
     * 设置快乐恶魂数据
     * @param data 快乐恶魂数据对象
     */
    void setHappyGhastData(HappyGhastData data);
}
