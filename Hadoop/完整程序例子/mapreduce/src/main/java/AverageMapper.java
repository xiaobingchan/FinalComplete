import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;

public class AverageMapper extends Mapper<LongWritable, Text, Text, Text> {
    //设置输出的key和value
    private Text outKey = new Text();
    private Text outValue = new Text();

    @Override
    protected void map(LongWritable key, Text value, Mapper<LongWritable, Text, Text, Text>.Context context) throws IOException, InterruptedException, IOException {

        //获取输入的行
        String line = value.toString();
        //取出无效记录
        if (line == null || line.equals("")) {
            return;
        }
        //对数据进行切分
        String[] splits = line.split("\t");

        //截取姓名和成绩
        String name = splits[0];
        String score1 = splits[2];
        String score2 = splits[3];
        String score3 = splits[4];
        String score4 = splits[5];
        String score5 = splits[6];
        String score6 = splits[7];
        String score=score1+score2+score3+score4+score5+score6;
        //设置输出的Key和value
        outKey.set(name);
        outValue.set(score);
        //将结果写出去
        context.write(outKey, outValue);

    }

}