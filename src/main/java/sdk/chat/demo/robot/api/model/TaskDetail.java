package sdk.chat.demo.robot.api.model;

public class TaskDetail {
    private String taskDate;
    private int index;
    private int status;

    // 任务掩码常量
    public static final int TASK_GW_MASK = 0b0001; // 第一位
    public static final int TASK_PRAY_MASK = 0b0010; // 第二位
    public static final int TASK_RECORD_MASK = 0b0100; // 第三位
    public static final int UNLOCK_STORY_MASK = 0b1000; // 第三位

    public TaskDetail(String taskDate) {
        this.taskDate = taskDate;
        this.index = 0;
        this.status = 0;
    }

    public String getTaskDate() {
        return taskDate;
    }

    public void setTaskDate(String taskDate) {
        this.taskDate = taskDate;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }


    /**
     * 检查指定任务是否完成
     *
     * @param taskMask 任务掩码 (TASK1_MASK, TASK2_MASK 或 TASK3_MASK)
     * @return true=已完成, false=未完成
     */
    public boolean isTaskCompleted(int taskMask) {
        return (status & taskMask) != 0;
    }

    /**
     * 设置任务完成状态
     *
     * @param taskMask  任务掩码
     * @param completed true=标记为完成, false=标记为未完成
     */
    public void setTaskCompleted(int taskMask, boolean completed) {
        if (completed) {
            status |= taskMask; // 设置对应位为1
        } else {
            status &= ~taskMask; // 设置对应位为0
        }
    }

    /**
     * 设置任务完成状态
     */
    public void completeTaskByIndex(int taskMaskIndex) {
        switch (taskMaskIndex) {
            case 0:
                status |= TASK_GW_MASK;
                break;
            case 1:
                status |= TASK_PRAY_MASK;
                break;
            case 2:
                status |= TASK_RECORD_MASK;
                break;
        }
    }

    /**
     * 获取当前所有任务状态
     *
     * @return 包含三个任务状态的数组
     */
    public boolean[] getAllTaskStatus() {
        return new boolean[]{
                isTaskCompleted(TASK_GW_MASK),
                isTaskCompleted(TASK_PRAY_MASK),
                isTaskCompleted(TASK_RECORD_MASK)
        };
    }

    public boolean isAllCompleted() {
        return isTaskCompleted(UNLOCK_STORY_MASK) && isAllUserTaskCompleted();
    }

    public boolean isAllUserTaskCompleted() {
        return isTaskCompleted(TASK_GW_MASK) && isTaskCompleted(TASK_PRAY_MASK) && isTaskCompleted(TASK_RECORD_MASK);
    }

    /**
     * 从状态值恢复对象
     *
     * @param statusValue 状态值
     */
    public void restoreFromStatus(int statusValue) {
        this.status = statusValue & 0b111; // 只保留低三位
    }

    /**
     * 已完成的打卡数
     */
    public int getCntComplete() {
        return isAllCompleted() ? index + 1 : index;
    }

    public void setIndexByCntComplete(int cntComplete) {
        index = isAllCompleted() ? cntComplete - 1 : cntComplete;
    }

}
