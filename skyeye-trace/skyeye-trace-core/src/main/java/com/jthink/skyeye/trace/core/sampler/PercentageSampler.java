package com.jthink.skyeye.trace.core.sampler;

import java.util.concurrent.atomic.AtomicLong;

/**
 * JThink@JThink
 *
 * @author JThink
 * @version 0.0.1
 * @desc 百分比采样率实现
 * 属于阶梯采样
 * 0-100条/s: 全部采集
 * 101-500条/s: 50%采集
 * 501条以上/s: 10%采集
 * @date 2017-02-15 09:57:39
 */
public class PercentageSampler implements Sampler {

    private AtomicLong count = new AtomicLong();//一秒内一共产生了多少条数据了
    private int levelOne = 100;
    private int levelTwo = 500;
    private Long lastTime = -1L;

    //是否采集--true表示要采样,false表示不采样该数据
    @Override
    public boolean isCollect() {
        boolean isSample = true;//默认是要采样的
        long n = count.incrementAndGet();//先获取该秒内有多少条数据了
        if (System.currentTimeMillis() - lastTime  < 1000) {//1000表示1秒,即当前时间还是在一秒内
            if (n > levelOne && n <= levelTwo) {//说明是符合101-500条/s条件,因此按照50%采集
                n = n % 2;//直接去模即可
                if (n != 0) {
                    isSample = false;
                }
            }
            if (n > levelTwo) {//符合501条以上/s: 10%采集条件
                n = n % 10;
                if (n != 0) {
                    isSample = false;
                }
            }
            //默认是true,表示1秒内产生了100条以内的数据,都采集
        } else {//说明超过一秒内了,重新计算
            count.getAndSet(0);
            lastTime = System.currentTimeMillis();
        }
        return isSample;
    }

    public static void main(String[] args) {
        PercentageSampler sampler = new PercentageSampler();
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; ++i) {
            System.out.println(String.valueOf(i) + sampler.isCollect());
        }
        long end = System.currentTimeMillis();
        System.out.println(end - start);
    }
}
