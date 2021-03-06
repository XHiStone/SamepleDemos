package com.hfxief.utils.fastjson;

import com.alibaba.fastjson.JSON;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import okhttp3.ResponseBody;
import retrofit2.Converter;

/** 
*@Title: FastJsonResponseBodyConverter
*@Description:  描述
*@date 2016/10/14 17:32
*@auther xie
*/
public class FastJsonResponseBodyConverter<T> implements Converter<ResponseBody, T> {

  private Type type;
  private Charset charset;

  public FastJsonResponseBodyConverter() {
  }

  public FastJsonResponseBodyConverter(Type type, Charset charset) {
    this.type = type;
    this.charset = charset;
  }

  @Override
  public T convert(ResponseBody value) throws IOException {
    try {
      return JSON.parseObject(value.string(), type);
    } finally {
      value.close();
    }
  }
}
