package com.cterm2.mcfm115.common

/**
  * 入出力のサイド定義
  * Directionからの変換は各種BlockEntityで定義する
  */
sealed trait GenericIOSides
object GenericIOSides {
    case object Input extends GenericIOSides
    case object Output extends GenericIOSides
    case object Invalid extends GenericIOSides
}
