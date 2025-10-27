package com.interview.patterns.adapter;

/**
 * 适配器模式 - 让不兼容的接口协同工作
 * 
 * 适用场景：
 * - 集成第三方库
 * - 遗留系统改造
 * - 接口不兼容问题
 */
public class AdapterPatternDemo {
    
    /**
     * 目标接口
     */
    interface MediaPlayer {
        void play(String audioType, String fileName);
    }
    
    /**
     * 高级媒体播放器接口
     */
    interface AdvancedMediaPlayer {
        void playVlc(String fileName);
        void playMp4(String fileName);
    }
    
    /**
     * VLC播放器
     */
    static class VlcPlayer implements AdvancedMediaPlayer {
        @Override
        public void playVlc(String fileName) {
            System.out.println("Playing vlc file: " + fileName);
        }
        
        @Override
        public void playMp4(String fileName) {
            // 不支持
        }
    }
    
    /**
     * MP4播放器
     */
    static class Mp4Player implements AdvancedMediaPlayer {
        @Override
        public void playVlc(String fileName) {
            // 不支持
        }
        
        @Override
        public void playMp4(String fileName) {
            System.out.println("Playing mp4 file: " + fileName);
        }
    }
    
    /**
     * 媒体适配器
     */
    static class MediaAdapter implements MediaPlayer {
        AdvancedMediaPlayer advancedMusicPlayer;
        
        public MediaAdapter(String audioType) {
            if (audioType.equalsIgnoreCase("vlc")) {
                advancedMusicPlayer = new VlcPlayer();
            } else if (audioType.equalsIgnoreCase("mp4")) {
                advancedMusicPlayer = new Mp4Player();
            }
        }
        
        @Override
        public void play(String audioType, String fileName) {
            if (audioType.equalsIgnoreCase("vlc")) {
                advancedMusicPlayer.playVlc(fileName);
            } else if (audioType.equalsIgnoreCase("mp4")) {
                advancedMusicPlayer.playMp4(fileName);
            }
        }
    }
    
    /**
     * 音频播放器
     */
    static class AudioPlayer implements MediaPlayer {
        MediaAdapter mediaAdapter;
        
        @Override
        public void play(String audioType, String fileName) {
            // 内置支持mp3
            if (audioType.equalsIgnoreCase("mp3")) {
                System.out.println("Playing mp3 file: " + fileName);
            }
            // 其他格式使用适配器
            else if (audioType.equalsIgnoreCase("vlc") || audioType.equalsIgnoreCase("mp4")) {
                mediaAdapter = new MediaAdapter(audioType);
                mediaAdapter.play(audioType, fileName);
            } else {
                System.out.println("Invalid media. " + audioType + " format not supported");
            }
        }
    }
    
    public static void main(String[] args) {
        AudioPlayer audioPlayer = new AudioPlayer();
        
        audioPlayer.play("mp3", "beyond the horizon.mp3");
        audioPlayer.play("mp4", "alone.mp4");
        audioPlayer.play("vlc", "far far away.vlc");
        audioPlayer.play("avi", "mind me.avi");
    }
}
