package sdk.chat.demo.robot.extensions;

public class StateStorage {
    //管理message的收藏标志
    // 使用数字24（二进制110）作为基础存储
    // 第一位(bit5)表示状态A（收藏原文），第二位(bit4)表示状态B(收藏AI返回的文字)
    private static final int BASE_VALUE = 6; // 二进制11000


    // 设置两个状态位
    public static int setStates(boolean stateA, boolean stateB) {
        return BASE_VALUE |
                ((stateA ? 1 : 0) << 5) |
                ((stateA ? 1 : 0) << 4);
    }

    // 翻转状态A（保持状态B不变）
    public static int toggleStateA(int currentState) {
        // 使用异或运算翻转第1位
        return currentState ^ (1 << 5);
    }

    // 翻转状态B（保持状态A不变）
    public static int toggleStateB(int currentState) {
        // 使用异或运算翻转第0位
        return currentState ^ (1 << 4);
    }

    // 单独设置状态A（保持状态B不变）
    public static int setStateA(int currentState, boolean stateA) {
        // 清除原来的状态A位，然后设置新值
        return (currentState & ~(1 << 5)) | ((stateA ? 1 : 0) << 5);
    }

    // 单独设置状态B（保持状态A不变）
    public static int setStateB(int currentState, boolean stateB) {
        // 清除原来的状态B位，然后设置新值
        return (currentState & ~(1 << 4)) | ((stateB ? 1 : 0) << 4);
    }

    // 获取状态A
    public static boolean getStateA(int storage) {
        return ((storage >> 5) & 1) == 1;
    }

    // 获取状态B
    public static boolean getStateB(int storage) {
        return ((storage >> 4) & 1) == 1;
    }

}
