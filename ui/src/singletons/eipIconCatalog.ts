import { EipId } from "../api/id"
import channelImg from "../assets/eip/channel.png"
import inboundAdapterImg from "../assets/eip/inbound_channel_adapter.png"
import outboundAdapterImg from "../assets/eip/outbound_channel_adapter.png"

type UrlCatalog = Record<string, Record<string, string>>

const eipIconUrls: UrlCatalog = {
  integration: {
    channel: channelImg,
    "publish-subscribe-channel": channelImg,
    "inbound-channel-adapter": inboundAdapterImg,
    "logging-channel-adapter": outboundAdapterImg,
  },
  jms: { "message-driven-channel-adapter": inboundAdapterImg },
}

const getIconUrl = (id: EipId) => eipIconUrls[id.namespace][id.name]

export default getIconUrl
