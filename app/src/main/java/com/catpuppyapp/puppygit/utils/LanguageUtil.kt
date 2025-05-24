package com.catpuppyapp.puppygit.utils

import android.content.Context
import com.catpuppyapp.puppygit.constants.LangCode
import com.catpuppyapp.puppygit.play.pro.R
import com.catpuppyapp.puppygit.utils.pref.PrefMan


object LanguageUtil {
    private const val TAG="LanguageUtil"

    private const val key = PrefMan.Key.lang

    val languageCodeList = listOf(
        LangCode.auto,

        // order by a-z
        LangCode.en,
        LangCode.ru,
        LangCode.tr,
        LangCode.zh_cn,

        // other language...
    )


    fun getLangCode(context: Context):String {
        return PrefMan.get(context, key, "")
    }

    fun setLangCode(context: Context, langCode:String) {
        PrefMan.set(context, key, langCode)
    }

    fun isAuto(langCode: String):Boolean {
        return langCode == LangCode.auto || langCode.isBlank()
    }

    /**
     * return true if `langCode` is not auto detected(empty string) and is supported
     */
    fun isSupportedLanguage(langCode:String, treatAutoAsUnsupported:Boolean=true):Boolean {
        // auto detected not represented any language, so return false
        if(treatAutoAsUnsupported && isAuto(langCode)) {
            return false
        }

        return languageCodeList.contains(langCode)
    }


    fun getLanguageTextByCode(languageCode:String, context: Context):String {
        if(languageCode.isBlank()) {
            return context.getString(R.string.auto)
//            return context.getString(R.string.follow_system)
        }

        // order by a-z
        if(languageCode == LangCode.en) {
            return context.getString(R.string.lang_name_english)
        }

        if(languageCode == LangCode.ru) {
            return context.getString(R.string.lang_name_russian)
        }

        if(languageCode == LangCode.zh_cn) {
            return context.getString(R.string.lang_name_chinese_simplified)
        }

        if(languageCode == LangCode.tr) {
            return context.getString(R.string.lang_name_turkish)
        }

        // add other language here


        // should never reach here, if user got unknown, just set to a supported language will resolved
        MyLog.w(TAG, "#getLanguageTextByCode: unknown language code '$languageCode'")
        return context.getString(R.string.unsupported)
    }

    /**
     * e.g. input "zh-rCN" return Pair("zh", "CN")
     */
    fun splitLanguageCode(languageCode:String):Pair<String,String> {
        val codes = languageCode.split("-r")
        if(codes.size>1) {
            return Pair(codes[0], codes[1])
        }else {
            return Pair(codes[0], "")
        }
    }


}
