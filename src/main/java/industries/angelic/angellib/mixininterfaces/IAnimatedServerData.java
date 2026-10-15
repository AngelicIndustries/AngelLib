package industries.angelic.angellib.mixininterfaces;

import industries.angelic.angellib.util.GifUtil;

public interface IAnimatedServerData {

    boolean getIsAnimatedIcon();

    void setIsAnimatedIcon(boolean val);

    GifUtil.StitchedAnimationData getStitchedAnimationData();

    void setStitchedAnimationData(GifUtil.StitchedAnimationData data);

}
