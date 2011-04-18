package me.lessis

object Instagram {
  val ClientId = "5e00b43c35494172a057944bac4a3bb5"
  def authorizeUrl =
    "https://api.instagram.com/oauth/authorize/?client_id=%s&response_type=token&redirect_uri=picsee://" format ClientId
  def taggedMedia(tag: String, token: String) =
    "https://api.instagram.com/v1/tags/%s/media/recent?access_token=%s" format(
      tag, token
    )
}
