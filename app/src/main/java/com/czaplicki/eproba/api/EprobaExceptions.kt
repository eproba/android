package com.czaplicki.eproba.api

class EprobaException(message: String, code: Int) : Exception("$message ($code)")
class EprobaNetworkException(message: String) : Exception(message)
class EprobaServerException(message: String, code: Int) : Exception("$message ($code)")
class EprobaClientException(message: String) : Exception(message)
class EprobaUnauthorizedException(message: String, code: Int) : Exception("$message ($code)")
