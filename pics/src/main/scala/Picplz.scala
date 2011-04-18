package me.lessis

object PicPlz {
  val ClientId = "CbRLCGphEJ3CPANX98SfucGvwNQ7VzR4"
  def authorizeUrl =
    "https://picplz.com/oauth2/authenticate?client_id=%s&response_type=code&redirect_uri=picsee://" format ClientId
}
