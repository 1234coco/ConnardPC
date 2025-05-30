import base64

def encoding_reversed(text:str):
    char_map = [[r"0",r"1",r"2",r"3",r"4",r"5",r"6",r"7",r"8",r"9",r"q",r"w",r"e",r"r",r"t",r"y",r"u",r"i",r"o",r"p",r"a",r"s",r"d",r"f",r"g",r"h",r"j",r"k",r"l",r"z",r"x",r"c",r"v",r"b",r"n",r"m",r"Q",r"W",r"E",r"R",r"T",r"Y",r"U",r"I",r"O",r"P",r"A",r"S",r"D",r"F",r"G",r"H",r"J",r"K",r"L",r"Z",r"X",r"C",r"V",r"B",r"N",r"M",r"~",r"`",r"!",r"@",r"#",r"$",r"%",r"^",r"&",r"*",r"(",r")",r"_",r"+",r"-",r"{",r"}",r"]",r"[","\\",r"|",r";",r":",r"/",r"<",r">",r"?",r" ",r"=",r".",r","],
                [0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92]]
    texts = list(char_map[0])
    # print(texts)
    texts.reverse()
    # print(list(char_map[1]))
    reversed_char_map = [texts,char_map[1]]
    # dem = ""
    # for i in range(len(char_map)):
    #     dem = dem + str(i)+","
    # print(dem)
    # print(reversed_char_map)
    text_en = text.encode()
    text_en = str(text_en)
    text_out=list()
    for x in text_en.replace("b'","").replace("'",""):
        for y in char_map[0]:
            if x == y:
                z = char_map[0].index(y)
                text_out.append(char_map[1][z])
                break
    text_end = ""
    for x in text_out:
        for y in reversed_char_map[1]:
            if x == y:
                z = reversed_char_map[1].index(y)
                text_end += str(reversed_char_map[0][z])
                break
    return text_end
def decoding_reversed(text:str):
    char_map = [[r"0",r"1",r"2",r"3",r"4",r"5",r"6",r"7",r"8",r"9",r"q",r"w",r"e",r"r",r"t",r"y",r"u",r"i",r"o",r"p",r"a",r"s",r"d",r"f",r"g",r"h",r"j",r"k",r"l",r"z",r"x",r"c",r"v",r"b",r"n",r"m",r"Q",r"W",r"E",r"R",r"T",r"Y",r"U",r"I",r"O",r"P",r"A",r"S",r"D",r"F",r"G",r"H",r"J",r"K",r"L",r"Z",r"X",r"C",r"V",r"B",r"N",r"M",r"~",r"`",r"!",r"@",r"#",r"$",r"%",r"^",r"&",r"*",r"(",r")",r"_",r"+",r"-",r"{",r"}",r"]",r"[","\\",r"|",r";",r":",r"/",r"<",r">",r"?",r" ",r"=",r".",r","],
                [0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92]]
    texts = list(char_map[0])
    # print(texts)
    texts.reverse()
    # print(list(char_map[1]))
    reversed_char_map = [texts,char_map[1]]
    # dem = ""
    # for i in range(len(char_map)):
    #     dem = dem + str(i)+","
    # print(dem)
    # print(reversed_char_map)
    text_en = text
    text_en = str(text_en)
    text_out=list()
    for x in text_en:
        for y in reversed_char_map[0]:
            if x == y:
                z = reversed_char_map[0].index(y)
                text_out.append(reversed_char_map[1][z])
                break
    text_end = ""
    for x in text_out:
        for y in char_map[1]:
            if x == y:
                z = char_map[1].index(y)
                text_end+=char_map[0][z]
                break
    text_end = bytes(text_end,"latin1")
    text_end = text_end.decode("unicode_escape").encode('latin1').decode("utf-8")
    return text_end
               
def encoding_half(text:str):
    text_ens = ""
    if ((len(text))%2)==0:
        text_en = text
    elif ((len(text))%2)==1:
        text_en = text[0:len(text)-1]
        text_ens = text[len(text)-1]
    count = 0
    trc = ""
    text_end = ""
    for i in text_en:
        if count % 2 == 0:
            trc = i
        elif count % 2 == 1:
            text_end += i + trc
        count+=1
    text_end = text_end + text_ens
    return text_end
def decoding_half(text:str):
    text_ens = ""
    if ((len(text))%2)==0:
        text_en = text
    elif ((len(text))%2)==1:
        text_en = text[0:len(text)-1]
        text_ens = text[len(text)-1]
    count = 0
    trc = ""
    text_end = ""
    for i in text_en:
        if count % 2 == 0:
            trc = i
        elif count % 2 == 1:
            text_end += i + trc
        count+=1
    text_end = text_end + text_ens
    return text_end
def encoding_base64(text:str):
    a = base64.b64encode(text.encode()).decode()
    return a
def decoding_base64(text:str):
    a = base64.b64decode(text.encode()).decode()
    return a
text = encoding_reversed("Ai di chuyển trước, người đó là GAY")
print(text)
text = encoding_base64(text)
print(text)
text = encoding_half(text)
print(text)
text = encoding_reversed(text)
print(text)
text = encoding_half(text)
print(text)
text = encoding_base64(text)
print(text)