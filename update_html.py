#!/usr/bin/env python3
import os
import re
import shutil
from pathlib import Path
from datetime import datetime

HTML_FILE = r"d:\Univer\Nam_3\HKII\CNVAR\DA_BookStore\BookStore\NgPhucs2375-DA_BookStore\src\main\resources\templates\main\Details_Produce.html"

def main():
    with open(HTML_FILE, 'r', encoding='utf-8') as f:
        content = f.read()

    # Replace Thường Được Mua Kèm
    pattern1 = r'(<h2 class="text-lg font-bold text-brand-dark mb-6 uppercase tracking-wide">Thường Được Mua Kèm</h2>\s*<div class="flex flex-col xl:flex-row items-center justify-between gap-8">\s*<div class="flex items-center gap-4 w-full xl:w-2/3 overflow-x-auto no-scrollbar pb-2">)(.*?)(<div class="w-full xl:w-1/3 flex flex-col xl:items-end border-t xl:border-t-0 xl:border-l border-brand-border pt-6 xl:pt-0 xl:pl-8">)'
    replacement1 = r'''\1
                <!-- Dynamic Bought Together Section -->
                <th:block th:if="${boughtTogetherBooks != null and !boughtTogetherBooks.isEmpty()}">
                    <th:block th:each="b, iterStat : ${boughtTogetherBooks}">
                        <div class="w-32 flex-shrink-0 flex flex-col items-center group cursor-pointer relative">
                            <a th:href="@{/book/{id}(id=${b.id})}" class="contents">
                                <div class="w-24 h-32 bg-[#2c3e50] border-4 border-white shadow-md flex flex-col items-center justify-center text-white p-2 mb-3 relative overflow-hidden group-hover:scale-105 transition-transform" th:style="'background-color:' + (${b.id % 2 == 0} ? '#2c3e50' : '#8e44ad')">
                                    <div class="text-xs font-serif font-bold text-center leading-tight text-yellow-400" th:text="${b.title}">BOOK TITLE</div>
                                </div>
                                <h4 class="text-xs font-bold text-center text-brand-dark line-clamp-1 group-hover:text-brand-brown" th:text="${b.title}">Book Name</h4>
                                <div class="text-brand-orange font-bold text-sm" th:text="${#numbers.formatDecimal(b.price, 0, 'COMMA', 0, 'POINT')} + 'đ'">100.000đ</div>
                            </a>
                        </div>
                        <div class="text-2xl text-gray-300 font-bold" th:if="${!iterStat.last}">+</div>
                    </th:block>
                </th:block>
                <div th:if="${boughtTogetherBooks == null or boughtTogetherBooks.isEmpty()}" class="text-gray-400 italic text-sm py-4">Chưa có dữ liệu mua kèm.</div>
            \3'''
    
    content = re.sub(pattern1, replacement1, content, flags=re.DOTALL)

    # Replace Dành Cho Bạn
    pattern2 = r'(<h2 class="text-2xl font-bold text-brand-dark uppercase tracking-wide border-b-2 border-brand-orange pb-3 -mb-\[2px\] inline-block">Dành Cho Bạn</h2>\s*<a href="#" class="text-sm font-bold text-brand-brown hover:underline">Xem thêm</a>\s*</div>\s*<div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-6 xl:gap-8">)(.*?)(</div>\s*</div>\s*</section>)'
    replacement2 = r'''\1
                <!-- Dynamic Similar Books Grid -->
                <th:block th:if="${similarBooks != null}">
                    <div class="product-card group cursor-pointer h-full" th:each="simBook : ${similarBooks}">
                        <a th:href="@{/book/{id}(id=${simBook.id})}" class="contents">
                            <div class="product-card-inner bg-white border border-brand-border p-4 h-full flex flex-col relative shadow-sm">
                                <div class="relative w-full h-56 bg-brand-brown/10 mb-4 flex items-center justify-center overflow-hidden border border-brand-brown/20">
                                    <div class="book-cover w-3/4 h-5/6 bg-[#34495e] border-[3px] border-white shadow-md flex items-center justify-center text-white text-center font-bold px-2 text-[10px]" th:text="${simBook.title}" th:style="'background-color:' + (${simBook.id % 3 == 0} ? '#c0392b' : '#2980b9')">BOOK</div>
                                </div>
                                <div class="flex-grow flex flex-col">
                                    <h3 class="text-sm font-bold text-brand-dark leading-tight mb-2 group-hover:text-brand-brown transition-colors line-clamp-2" th:text="${simBook.title}">Name</h3>
                                    <div class="mt-auto flex flex-col gap-1">
                                        <div class="text-brand-dark font-medium text-xs">Giá: <span class="text-brand-orange text-base font-bold" th:text="${#numbers.formatDecimal(simBook.price, 0, 'COMMA', 0, 'POINT')} + 'đ'">100.000đ</span></div>
                                    </div>
                                </div>
                            </div>
                        </a>
                    </div>
                </th:block>
            \3'''

    content = re.sub(pattern2, replacement2, content, flags=re.DOTALL)

    with open(HTML_FILE, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Done")

if __name__ == "__main__":
    main()