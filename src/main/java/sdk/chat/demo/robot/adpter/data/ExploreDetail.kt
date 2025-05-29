package sdk.chat.demo.robot.adpter.data

import com.google.gson.JsonParser

class ExploreDetail {
    var text: String? = null
    var action: Int = 0
    var params: String? = null

    companion object {
        @JvmStatic
        fun loads(exploreStr: String): ExploreDetail? {
            try {
                val data: ExploreDetail = ExploreDetail()
                val items = JsonParser.parseString(exploreStr).getAsJsonArray()
                val len = items.size()
                if (len > 0) {
                    data.text = items.get(0).asString
                }
                if (len > 1) {
                    data.action = items.get(1).asInt
                }
                if (len > 2) {
                    data.params = items.get(2).asString
                }
                return data
            } catch (ignored: Exception) {
            }
            return null
        }
    }
}