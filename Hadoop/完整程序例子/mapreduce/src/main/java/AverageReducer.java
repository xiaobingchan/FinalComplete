import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

public class AverageReducer extends Reducer<Text, Text, Text, FloatWritable> {
    //定义写出去的Key和value
    private Text name = new Text();
    private FloatWritable avg = new FloatWritable();

    @Override
    protected void reduce(Text key, Iterable<Text> value, Reducer<Text, Text, Text, FloatWritable>.Context context) throws IOException, InterruptedException, IOException {
        //定义科目数量
        int courseCount = 6;
        //定义中成绩
        int sum = 0;
        //定义平均分
        float average = 0;

        //遍历集合求总成绩
        for (Text val : value) {
            sum += Integer.parseInt(val.toString());
            courseCount++;
        }

        //求平均成绩
        average = sum / courseCount;

        //设置写出去的名字和成绩
        name.set(key);
        avg.set(average);

        //把结果写出去
        context.write(name, avg);
    }
}