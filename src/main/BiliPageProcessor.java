package main;

import ado.BiliUserAdo;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.selector.JsonPathSelector;
import us.codecraft.webmagic.utils.HttpConstant;

import java.util.HashMap;
import java.util.Map;

/**
 * create by Intellij IDEA
 * Author: Al-assad
 * E-mail: yulinying_1994@outlook.com
 * Github: https://github.com/Al-assad
 * Date: 2017/4/11 11:54
 * Description: 程序运行的入口；
 *              实现PageProcessor接口，负责目标url的抽取逻辑；
 */

//TODO:ADO层

public class BiliPageProcessor implements PageProcessor{

    //构建Site对象，指定请求头键值字段
    private Site site = Site.me()
            .setRetryTimes(3)
            .setTimeOut(30000)
            .setSleepTime(1500)     //跟据试验，http://space.bilibili.com/ajax/member/GetInfo接口有IP接入限制，估计是60s内上限150次
            .setCycleRetryTimes(3)
            .setUseGzip(true)
            .addHeader("Host","space.bilibili.com")
            .addHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; WOW64; rv:52.0) Gecko/20100101 Firefox/52.0")
            .addHeader("Accept","application/json, text/plain, */*")
            .addHeader("Accept-Language","zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3")
            .addHeader("Accept-Encoding","gzip, deflate, br")
            .addHeader("X-Requested-With","XMLHttpRequest")
            .addHeader("Content-Type","application/x-www-form-urlencoded")
            .addHeader("Referer","http://space.bilibili.com/10513807/");

    private static final long BEGIN_MID = 1;    //开始用户mid
    private static final long END_MID = 100300000;      //结束用户mid,(2017-04的估计注册用户数)

    private BiliUserAdo biliUserDao = new BiliUserAdo();   //持久化对象


    @Override
    public void process(Page page) {

        String pageRawText = page.getRawText();
        //跳过连接失败页
        if(new JsonPathSelector("$.status").select(pageRawText).equals("false"))
            page.setSkip(true);

        //使用jsonPath获取json中的有效数据，并装载入BiliUser对象
        BiliUser user = new BiliUser();

        user.setMid(Long.parseLong(new JsonPathSelector("$.data.mid").select(pageRawText)));
        user.setName(new JsonPathSelector("$.data.name").select(pageRawText));
        user.setSex(new JsonPathSelector("$.data.sex").select(pageRawText));
        user.setLevel(Integer.parseInt(new JsonPathSelector("$.data.level_info.current_level").select(pageRawText)));
        user.setSign(new JsonPathSelector("$.data.sign").select(pageRawText));
        user.setFaceUrl( new JsonPathSelector("$.data.face").select(pageRawText));
        user.setFriends(Integer.parseInt(new JsonPathSelector("$.data.friend").select(pageRawText)));
        user.setFans(Integer.parseInt(new JsonPathSelector("$.data.fans").select(pageRawText)));
        user.setPlayNum(Integer.parseInt(new JsonPathSelector("$.data.playNum").select(pageRawText)));
        user.setBirthday(new JsonPathSelector("$.data.birthday").select(pageRawText));
        user.setPlace(new JsonPathSelector("$.data.place").select(pageRawText));

        System.out.println("\n"+user);

        biliUserDao.saveUser(user);    //保存BiliUser对象到数据库

    }

    @Override
    public Site getSite() {
        return site;
    }


    //运行主方法
    public static void main(String[] args){

        Spider spider = Spider.create(new BiliPageProcessor());
        //添加请求对象序列
        long mid;
        for(mid = BEGIN_MID; mid < END_MID; mid++){

                //构造post请求数据组和url
                Map<String, Object> nameValuePair = new HashMap<String, Object>();
                NameValuePair[] values = new NameValuePair[1];
                values[0] = new BasicNameValuePair("mid", String.valueOf(mid));
                nameValuePair.put("nameValuePair", values);
                String url = "http://space.bilibili.com/ajax/member/GetInfo?mid="+mid;   //bilibili用户信息获取接口
                //构造Request请求对象
                Request request = new Request(url);
                request.setExtras(nameValuePair);
                request.setMethod(HttpConstant.Method.POST);
                //向Spider对象添加Request对象
                spider.addRequest(request);

        }

        spider.thread(2).run();  //启动60个线程



    }
}
