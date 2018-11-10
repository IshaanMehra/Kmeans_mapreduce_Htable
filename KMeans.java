import java.io.*;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;
import java.net.URI;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.*;


import org.apache.hadoop.mapreduce.*;

import org.apache.hadoop.mapreduce.lib.input.*;
import org.apache.hadoop.mapreduce.lib.output.*;




class Point implements WritableComparable <Point>{
    public static final int DIMENTION = 2;
    public double x;
    public double y;
    public double[] arr;
    Point(Point p){}
    Point(){}
    Point ( double xaxis,double yaxis ) {
        x = xaxis; y= yaxis;
    }
    
    public static double getEulerDist(Point vec1,Point vec2)
    {
        if(!(vec1.arr.length==DIMENTION && vec2.arr.length==DIMENTION))
        {
            System.exit(1);
        }
        double dist=0.0;
        for(int i=0;i<DIMENTION;++i)
        {
            dist+=(vec1.arr[i]-vec2.arr[i])*(vec1.arr[i]-vec2.arr[i]);
        }
        return Math.sqrt(dist);
    }

    public void readFields(DataInput in) throws IOException {
        // TODO Auto-generated method stub
        
        x =in.readDouble();
        y =in.readDouble();
      
        
    }
    public void write(DataOutput out) throws IOException {
        // TODO Auto-generated method stub
        
           out.writeDouble(x);
           out.writeDouble(y);
             
        
    }

    public int compareTo(Point other) {
        // TODO Auto-generated method stub
        int comp = Double.valueOf(this.x).compareTo(Double.valueOf(other.x));
        if (comp==0)
        {
        comp = Double.valueOf(this.y).compareTo(Double.valueOf(other.y));
        
    }
    return comp;

    }
    public String toString() {
        return x +" "+ y;
    }
}

class Avg implements Writable{
       public double sumX;
        public double sumY;
        public long count;
    Avg(Point a){}
    Avg(){}
    Avg ( double xsum,double ysum,long countk ) {
        sumX = xsum; sumY= ysum;count =countk;
    }


    public void readFields(DataInput in) throws IOException {
        // TODO Auto-generated method stub
        
        sumX =in.readDouble();
        sumY =in.readDouble();
        count =in.readLong();
      
        
    }
    public void write(DataOutput out) throws IOException {
        // TODO Auto-generated method stub
        
           out.writeDouble(sumX);
           out.writeDouble(sumY);
           out.writeLong(count);
             
        
    }


 
  
}

public class KMeans {
    static Vector<Point> centroids = new Vector<Point>(100);
  //  static Hashtable<Point,Avg> Htable = new Hashtable<Point,Avg>();
       static Hashtable<Point,Avg> Htable = new Hashtable<Point,Avg>();
   
    public static String LINECUTTER = "\t| ";
    public static class AvgMapper extends Mapper<Object,Text,Point,Avg> {
        int k = 0;
        @Override
        public void setup(Context context) throws IOException, InterruptedException
        {
        
            Htable = new Hashtable<Point,Avg>();
            URI[] paths = context.getCacheFiles();
            Configuration conf = context.getConfiguration();
            FileSystem fs = FileSystem.get(conf);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fs.open(new Path(paths[0]))));
            String l ;
        try {
            while((l=reader.readLine())!=null) {
            
                String[] pval =l.split(",");
                double point1 = Double.parseDouble(pval[0]);
                double point2 = Double.parseDouble(pval[1]);
                Point pointnew =new Point(point1,point2);
                centroids.add(pointnew);
               
          
            } 
        
        }
        finally {
            reader.close();
        }
        for(Point c:centroids) {
            Htable.put(c, new Avg(0.0,0.0,0));
        }
    
    }
        @Override
        public void cleanup(Context context) throws IOException, InterruptedException
        //  mapper cleanup:
        //        for each key c in table
        //            emit(c,table[c])
        
        {
              Set<Point> keys = Htable.keySet();
              for(Point key: keys){
                  Avg avg=Htable.get(key);
                  context.write(new Point(key.x,key.y),Htable.get(key));
              }
      
            
       
              
    }  
        @Override
        public void map(Object key, Text value,
                Context context ) throws IOException, InterruptedException
        {  
            int count =0;
            int index = Integer.MAX_VALUE;
            double min_dist = Double.MAX_VALUE;
        String s = value.toString();         
        String[] str = s.split(",");
        double x1 = Double.parseDouble(str[0]);
        double y1 = Double.parseDouble(str[1]);
        Point point = new Point(x1, y1);  
      
        Double[] arr1 =new Double[2];
        arr1[0]=point.x;
        arr1[1]=point.y;
        for(Point centroid:centroids)
        {
        Double[] arr2=new Double[2];
        arr2[0]=centroid.x;
        arr2[1]=centroid.y;
        double euclidean_dist=Math.abs(Math.sqrt(((arr2[0]-arr1[0])*(arr2[0]-arr1[0]))+((arr2[1]-arr1[1])*(arr2[1]-arr1[1]))));
        if(euclidean_dist<min_dist)
        {   
                index=count;
            min_dist=euclidean_dist;
            

        }
         // System.out.println(count);
        count++;          
        }
        Point x =centroids.get(index);
        Avg av = Htable.get(x);
        
        if(av.count==0)
        {
            Htable.put(x, new Avg(x1,y1,1) );
       //            else table[c] = new Avg(table[c].sumX+x,table[c].sumY+y,table[c].count+1)
        }
        else
        {
        Htable.put(x,new Avg((av.sumX+x1),(av.sumY+y1),(av.count+1)));
        }
        
        
          
        }
        
    
    }

    public static class AvgReducer extends Reducer<Point,Avg,Text,Object> {
        @Override
        public void reduce(Point centr,Iterable<Avg> point,Context context) throws IOException,InterruptedException
        {
    
        int  count = 0;
        double sx=0.0000000;
        double sy=0.0000000;
        for(Avg p:point)
        {
            count +=  p.count;
            sx += p.sumX;
            sy += p.sumY;
        }
        centr.x = sx/(double)count;
        centr.y = sy/(double)count;
        Point finalcent = new Point(centr.x,centr.y);
        try {
        context.write(new Text(finalcent.toString()),NullWritable.get());
        }
        catch(Exception e) {
             System.out.println(e);
        }
        
        }       
    }

    public static void main ( String[] args ) throws Exception {
 
        String datafile = args[0];
        String centroidfile = args[1];
      
        

        Job job = Job.getInstance();
        job.setJobName("Kmeans");
        job.setJarByClass(KMeans.class);
        job.setMapperClass(AvgMapper.class);
        job.setReducerClass(AvgReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(NullWritable.class);
        job.setMapOutputKeyClass(Point.class);
        job.setMapOutputValueClass(Avg.class);
        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);      
        FileInputFormat.setInputPaths(job,new Path (args[0]));
        FileOutputFormat.setOutputPath(job,new Path(args[2]));     
        job.addCacheFile(new URI(args[1]));
        job.waitForCompletion(true);
   }
  
}
