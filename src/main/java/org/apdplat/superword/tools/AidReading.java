/*
 * APDPlat - Application Product Development Platform
 * Copyright (c) 2013, 杨尚川, yang-shangchuan@qq.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.apdplat.superword.tools;

import org.apdplat.superword.model.Word;
import org.apdplat.superword.tools.WordLinker.Dictionary;
import org.apdplat.word.WordSegmenter;
import org.apdplat.word.segmentation.SegmentationAlgorithm;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 辅助阅读:
 * 以电影功夫熊猫使用的单词分析为例
 * 你英语四级过了吗? 功夫熊猫看了吗?
 * 去除停用词后,功夫熊猫使用了804个英语单词,你会说很简单吧,别急,这些单词中仍然有186个单词不在四级词汇表中,花两分钟时间看看你是否认识这些单词.
 * Created by ysc on 11/15/15.
 */
public class AidReading {
    private static final Set<Word> STOP_WORDS = WordSources.get("/word_stop.txt");

    public static void main(String[] args) throws IOException {
        WordLinker.serverRedirect = null;
        String result = analyse(Arrays.asList("/it/movie/kungfupanda.txt"),
                WordSources.get("/word_CET4.txt"), Dictionary.ICIBA, 6);
        //String result = analyse(Arrays.asList("/it/movie/kungfupanda.txt", "/it/movie/kungfupanda2.txt"),
        //        WordSources.get("/word_CET4.txt"), Dictionary.ICIBA, 6);
        System.out.println(result);
    }
    public static String analyse(List<String> resources, Set<Word> words, Dictionary dictionary, int column) {
        return analyse(resources, words, dictionary, column, false, null);
    }
    public static String analyse(List<String> resources, Set<Word> words, Dictionary dictionary, int column, boolean searchOriginalText, String book) {
        StringBuilder result = new StringBuilder();
        Map<String, AtomicInteger> map = new ConcurrentHashMap<>();
        resources.forEach(resource -> {
            try {
                FileUtils.readResource(resource).forEach(line -> {
                    StringBuilder buffer = new StringBuilder();
                    for(org.apdplat.word.segmentation.Word term : WordSegmenter.segWithStopWords(line, SegmentationAlgorithm.PureEnglish)){
                        String word = term.getText();

                        if (word.contains("'")) {
                            continue;
                        }
                        buffer.setLength(0);
                        for (char c : word.toCharArray()) {
                            if (Character.isAlphabetic(c)) {
                                buffer.append(Character.toLowerCase(c));
                            }
                        }
                        String baseForm = IrregularVerbs.getBaseForm(buffer.toString());
                        buffer.setLength(0);
                        buffer.append(baseForm);
                        if(buffer.length()<2 || buffer.length() > 14){
                            continue;
                        }
                        if(STOP_WORDS.contains(new Word(buffer.toString(), ""))){
                            continue;
                        }
                        map.putIfAbsent(buffer.toString(), new AtomicInteger());
                        map.get(buffer.toString()).incrementAndGet();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        List<String> list = new ArrayList<>();

        map.entrySet().stream().sorted((a, b) -> b.getValue().get() - a.getValue().get()).forEach(entry -> {
            String w = entry.getKey();
            if (words.contains(new Word(w, ""))) {
                return;
            }
            if (w.endsWith("s") && words.contains(new Word(w.substring(0, w.length() - 1), ""))) {
                return;
            }
            if (w.endsWith("es") && words.contains(new Word(w.substring(0, w.length() - 2), ""))) {
                return;
            }
            if (w.endsWith("ies") && words.contains(new Word(w.substring(0, w.length() - 3)+"y", ""))) {
                return;
            }
            if (w.endsWith("ed") && words.contains(new Word(w.substring(0, w.length() - 1), ""))) {
                return;
            }
            if (w.endsWith("ed") && words.contains(new Word(w.substring(0, w.length() - 2), ""))) {
                return;
            }
            if (w.endsWith("ied") && words.contains(new Word(w.substring(0, w.length() - 3)+"y", ""))) {
                return;
            }
            if (w.endsWith("ing") && words.contains(new Word(w.substring(0, w.length() - 3), ""))) {
                return;
            }
            if (w.endsWith("er") && words.contains(new Word(w.substring(0, w.length() - 2), ""))) {
                return;
            }
            if (w.endsWith("est") && words.contains(new Word(w.substring(0, w.length() - 3), ""))) {
                return;
            }
            String originalText = "";
            if(searchOriginalText){
                originalText = "\t<a target=\"_blank\" href=\"aid-reading-detail.jsp?book="+book+"&word="+entry.getKey()+"&dict=ICIBA&pageSize="+entry.getValue()+"\">[" + entry.getValue() + "]</a>";
            }else{
                originalText = "\t[" + entry.getValue() + "]";
            }
            list.add(WordLinker.toLink(entry.getKey(), dictionary) + originalText);
        });
        result.append("<h3>words don't occur in specified set: ("+list.size()+") </h3>\n");
        result.append(HtmlFormatter.toHtmlTableFragment(list, column));

        list.clear();

        map.entrySet().stream().sorted((a, b) -> b.getValue().get() - a.getValue().get()).forEach(entry -> {
            String originalText = "";
            if(searchOriginalText){
                originalText = "\t<a target=\"_blank\" href=\"aid-reading-detail.jsp?book="+book+"&word="+entry.getKey()+"&dict=ICIBA&pageSize="+entry.getValue()+"\">[" + entry.getValue() + "]</a>";
            }else{
                originalText = "\t[" + entry.getValue() + "]";
            }
            list.add(WordLinker.toLink(entry.getKey(), dictionary) + originalText);
        });
        result.append("<h3>words: (" + list.size() + ") </h3>\n");
        result.append(HtmlFormatter.toHtmlTableFragment(list, column));
        return result.toString();
    }
}