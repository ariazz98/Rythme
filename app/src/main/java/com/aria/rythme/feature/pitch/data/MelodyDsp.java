package com.aria.rythme.feature.pitch.data;

/** 与独立样本核验过的 16 kHz / 1024 FFT / 160 hop 前端；无录音、播放或网络操作。 */
final class MelodyDsp {
    static final int FFT=1024,HOP=160,MELS=128;
    static float[] melBasis() {
        double low=2595*Math.log10(1+30.0/700), high=2595*Math.log10(1+8000.0/700);
        double[] edges=new double[MELS+2];
        for(int i=0;i<edges.length;i++)edges[i]=700*(Math.pow(10,(low+(high-low)*i/(MELS+1))/2595)-1);
        float[] basis=new float[MELS*(FFT/2+1)];
        for(int m=0;m<MELS;m++)for(int k=0;k<=FFT/2;k++) {
            double hz=k*16000.0/FFT;
            double triangle=Math.max(0,Math.min((hz-edges[m])/(edges[m+1]-edges[m]),(edges[m+2]-hz)/(edges[m+2]-edges[m+1])));
            basis[m*(FFT/2+1)+k]=(float)(triangle*2/(edges[m+2]-edges[m]));
        }
        return basis;
    }
    static float[] resample(float[] input,int sr,Runnable check){
        if(sr==16000)return input;
        float[] output=new float[(int)Math.round(input.length*16000.0/sr)];double cutoff=Math.min(1,16000.0/sr)*.94;
        for(int i=0;i<output.length;i++){
            if(i%4096==0)check.run();
            double position=(double)i*sr/16000.0,sum=0,weight=0;int center=(int)Math.floor(position);
            for(int j=center-31;j<=center+32;j++){
                if(j<0||j>=input.length)continue;
                double d=position-j;if(Math.abs(d)>32)continue;
                double x=Math.PI*cutoff*d;
                double w=cutoff*(Math.abs(x)<1e-12?1:Math.sin(x)/x)*(.5+.5*Math.cos(Math.PI*d/32));
                sum+=input[j]*w;weight+=w;
            }
            output[i]=(float)(sum/weight);
            if(!Float.isFinite(output[i]))throw new IllegalArgumentException("Non-finite resampled sample at "+i);
        }
        return output;
    }
    static int reflect(int i,int n){if(n<2)return 0;while(i<0||i>=n)i=i<0?-i:2*n-2-i;return i;}
    static void fft(double[] re,double[] im){
        for(int i=1,j=0;i<FFT;i++){
            int bit=FFT>>1;for(;(j&bit)!=0;bit>>=1)j^=bit;j^=bit;
            if(i<j){double x=re[i];re[i]=re[j];re[j]=x;}
        }
        for(int len=2;len<=FFT;len<<=1){
            double angle=-2*Math.PI/len,stepR=Math.cos(angle),stepI=Math.sin(angle);
            for(int start=0;start<FFT;start+=len){double wr=1,wi=0;
                for(int j=0;j<len/2;j++){
                    int a=start+j,b=a+len/2;double br=re[b]*wr-im[b]*wi,bi=re[b]*wi+im[b]*wr;
                    re[b]=re[a]-br;im[b]=im[a]-bi;re[a]+=br;im[a]+=bi;
                    double next=wr*stepR-wi*stepI;wi=wr*stepI+wi*stepR;wr=next;
                }
            }
        }
    }
    static float[][] mel(float[] pcm,Runnable check){
        float[] basis=melBasis();
        int count=1+pcm.length/HOP;float[][] result=new float[MELS][count];
        double[] window=new double[FFT];for(int i=0;i<FFT;i++)window[i]=.5-.5*Math.cos(2*Math.PI*i/FFT);
        double[] re=new double[FFT],im=new double[FFT],mag=new double[FFT/2+1];
        for(int frame=0;frame<count;frame++){
            if(frame%32==0)check.run();
            for(int j=0;j<FFT;j++){re[j]=pcm[reflect(frame*HOP+j-FFT/2,pcm.length)]*window[j];im[j]=0;}
            fft(re,im);for(int j=0;j<mag.length;j++)mag[j]=Math.hypot(re[j],im[j]);
            for(int m=0;m<MELS;m++){
                double sum=0;for(int j=0;j<mag.length;j++){float w=basis[m*mag.length+j];if(w!=0)sum+=w*mag[j];}
                result[m][frame]=(float)Math.log(Math.max(1e-5,sum));
            }
        }
        return result;
    }
    static float[][] melFrames(float[] pcm, int offset, int totalSamples, int begin, int count, Runnable check) {
        float[] basis=melBasis(); float[][] result=new float[MELS][count];
        double[] window=new double[FFT];for(int i=0;i<FFT;i++)window[i]=.5-.5*Math.cos(2*Math.PI*i/FFT);
        double[] re=new double[FFT],im=new double[FFT],mag=new double[FFT/2+1];
        for(int f=0;f<count;f++) {
            check.run();int frame=begin+f;
            if(frame<0||frame>=1+totalSamples/HOP)continue;
            for(int j=0;j<FFT;j++){re[j]=pcm[reflect(frame*HOP+j-FFT/2,totalSamples)-offset]*window[j];im[j]=0;}
            fft(re,im);for(int j=0;j<mag.length;j++)mag[j]=Math.hypot(re[j],im[j]);
            for(int m=0;m<MELS;m++){
                double sum=0;for(int j=0;j<mag.length;j++){float w=basis[m*mag.length+j];if(w!=0)sum+=w*mag[j];}
                result[m][f]=(float)Math.log(Math.max(1e-5,sum));
            }
        }
        return result;
    }

}
