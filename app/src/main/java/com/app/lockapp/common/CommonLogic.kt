package com.app.lockapp.common

import android.util.Log


fun commonTranslateTimeIntToString(hour:Int?, minute:Int?):String{

    return if(hour==null||minute==null){
        ""
    }else{
        commonTranslateNumberIntTo2DigitString(hour)+":"+commonTranslateNumberIntTo2DigitString(minute)
    }
}

fun commonTranslateNumberIntTo2DigitString(number:Int):String{
    return if(number>9){number.toString()}else{
        "0$number"
    }
}