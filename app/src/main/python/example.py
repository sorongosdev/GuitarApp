# Project name: Polyphonic note detector using Harmonic Product Spectrum
# Date:         2021.05.19
# Author:       João Nuno Carvalho
# Description:  This is my implementation of a polyphonic note detector using
#               the Harmonic Product Spectrum method.
#               The input is a mono WAV file.
#               The output are the corresponding notes in time.
# License: MIT Open Source License

import numpy as np
import matplotlib.pyplot as plt
import wave
import math
import io

path     = "./"
filename = '110-C7-DXDU.wav'

note_threshold = 5_000.0    # 120   # 50_000.0   #  3_000.0

# Parameters
sample_rate  = 44100                     # Sampling Frequency
fft_len      = 8820    # 8820-100bpm # 8192-110bpm    # Length of the FFT window
overlap      = 0.5                       # Hop overlap percentage between windows
hop_length   = int(fft_len*(1-overlap))  # Number of samples between successive frames

# For the calculations of the music scale.
TWELVE_ROOT_OF_2 = math.pow(2, 1.0 / 12)

## wav 파일 읽은 후, sample_rate와 input_buffer 반환
# (sample_rate : int, input_buffer : NDArray[Any]) 반환, NDArray[Any]는 실수값의 numpy 배열을 의미
def read_wav_file(wave_bytes):
    wave_file = io.BytesIO(wave_bytes)
    with wave.open(wave_file, 'rb') as wf:
        params = wf.getparams()
        frames = wf.readframes(params.nframes)

    #     wav_handler = wave.open(path + filename,'rb')    # 지정된 경로에 wav 파일을 읽기 전용 모드로 연다.
    #     num_frames = wav_handler.getnframes()            # 파일에서 sample의 총 개수를 얻는다. 44100*(wav 길이 예로 4초) = 176400개
    #     sample_rate = wav_handler.getframerate()         # 파일의 sample_rate를 얻는다. 44100
    sample_rate = 44100
    #     wav_frames = wav_handler.readframes(num_frames)  # 모든 frame을 읽는다. wav_frames는 num_frames의 두 배이다. 각 샘플이 2바이트로 표현되기 때문. wav_frames의 바이트 배열 길이는 176400*2 = 352800 바이트이다.
    wav_frames = frames

    signal_temp = np.frombuffer(wav_frames, np.int16) # 읽은 wav_frame 데이터를 numpy 배열로 변환한다. 데이터 타입은 int16이다.
    signal_array = np.zeros(len(signal_temp), float) # wav_frames로부터 생성된 numpy 배열이다. 신호를 저장할 float 타입의 numpy 배열을 생성한다.

    for i in range(0, len(signal_temp)):
        signal_array[i] = signal_temp[i] / (2.0**15) # int16 타입의 값을 [-1, 1] 범위의 float64 타입으로 변환합니다.

    print("------------------------------")
    #     print("file_name: " + str(filename))
    print("sample_rate: " + str(sample_rate) + " Hz")
    print("input_buffer.size(sample의 총 갯수): " + str(len(signal_array)) + " 개")
    print("seconds(input_buffer.size/sample_rate): " + to_str_f4(len(signal_array)/sample_rate) + " s")
    print("type [-1, 1]: " + str(signal_array.dtype))
    print("min: " + to_str_f4(np.min(signal_array)) + " max: " + to_str_f4(np.max(signal_array))  )

    # sample_rate int형 숫자와 input_buffer numpy float 배열 반환
    return sample_rate, signal_array

## chunk 나눈 후, 나누어진 chunk의 리스트 반환
# (나누어진 청크들의 리스트 : list[NDArray]) 반환
# buffer의 총 sample 수 = wav 파일 seconds * fft_len
def divide_buffer_into_non_overlapping_chunks(buffer, max_len): # max_len -> fft_len
    buffer_len = len(buffer)                  # input_buffer의 길이 계산, buffer에 총 몇개의 sample이 있는지를 반환
    chunks = int(buffer_len / max_len)        # input_buffer 길이를 fft_len으로 나누어 몇 개의 chunk로 나눌 수 있는지 계산

    division_pts_list = []                    # chunk를 나눌 지점을 저장할 리스트
    for i in range(1, chunks):
        division_pts_list.append(i * max_len) # 각 청크의 시작 지점을 계산하여 리스트에 추가, fft_len의 배수가 리스트에 추가됨 -> [22050, 44100, 66150, ..]
    splitted_array_view = np.split(buffer, division_pts_list, axis=0) # 계산된 지점을 기준으로 버퍼를 나눈다.

    print("------------------------------")
    print("buffers_num: " + str(chunks))      # 총 chunks의 개수를 출력
    # print("나누어진 chunk들에 대한 리스트 :", splitted_array_view)
    # 나누어진 청크들의 리스트를 반환, list[NDArray]
    return splitted_array_view

## fft 연산 후, frequency 배열과 magnitude 배열과 frequency 개수(대칭적인 rfft 이용) 반환
# (frequency 배열 : NDArray[floating[Any]], magnitude 배열 : NDArray[Any], frequency 개수 : int) 반환
def getFFT(data, rate):
    # Returns fft_freq and fft, fft_res_len.
    len_data = len(data)                # 입력 데이터의 길이 계산
    data = data * np.blackman(len_data)  # 입력 데이터에 hamming_window를 적용하여 스펙트럼의 누설을 감소

    # fft 연산 후 magnitude 배열
    fft = np.fft.rfft(data)             # 입력 데이터에 대해 실수 fft를 수행한다. fft의 결과로 복소수 numpy 배열을 반환한다.
    # rfft는 수행하면 양수, 음수의 대칭적이므로 양수만을 출력한다.
    fft = np.abs(fft)                   # fft 결과의 절대값을 취하여 magnitue를 얻는다. 실수값의 numpy 배열을 반환한다.

    # fft 연산 후 frequency 개수
    ret_len_FFT = len(fft)              # fft 결과의 길이를 저장한다. 배열의 원소 개수, 즉 fft 변환을 통해 분석된 주파수 성분의 개수를 반환한다.

    # fft 연산 후 frequency 배열
    freq = np.fft.rfftfreq(len_data, 1.0 / sample_rate) # fft 결과에 대응하는 주파수 배열을 계산한다.
    # return ( freq[:int(len(freq) / 2)], fft[:int(ret_len_FFT / 2)], ret_len_FFT )

    # print("--------------getFFT() 거친 후----------------")
    # print("fft 연산 후 magnitude 배열 :", fft)
    # print("fft 연산 후 frequency 개수 :", ret_len_FFT)
    # print("fft 연산 후 frequency 배열 :", freq)
    # (frequency 배열 : NDArray[floating[Any]], magnitude 배열 : NDArray[Any], frequency 개수 : int) 반환
    return (freq, fft, ret_len_FFT)

## fft 결과에 DC offset을 제거한 magnitude를 반환
def remove_dc_offset(fft_res):
    # Removes the DC offset from the FFT (First bin's)
    fft_res[0] = 0.0
    fft_res[1] = 0.0
    fft_res[2] = 0.0
    return fft_res

def freq_for_note(base_note, note_index):
    # See Physics of Music - Notes
    #     https://pages.mtu.edu/~suits/NoteFreqCalcs.html

    A4 = 440.0

    base_notes_freq = {"A2" : A4 / 4,   # 110.0 Hz
                       "A3" : A4 / 2,   # 220.0 Hz
                       "A4" : A4,       # 440.0 Hz
                       "A5" : A4 * 2,   # 880.0 Hz
                       "A6" : A4 * 4 }  # 1760.0 Hz

    scale_notes = { "C"  : -9.0,
                    "C#" : -8.0,
                    "D"  : -7.0,
                    "D#" : -6.0,
                    "E"  : -5.0,
                    "F"  : -4.0,
                    "F#" : -3.0,
                    "G"  : -2.0,
                    "G#" : -1.0,
                    "A"  :  1.0,
                    "A#" :  2.0,
                    "B"  :  3.0,
                    "Cn" :  4.0}

    scale_notes_index = list(range(-9, 5)) # Has one more note.
    note_index_value = scale_notes_index[note_index]
    freq_0 = base_notes_freq[base_note]
    freq = freq_0 * math.pow(TWELVE_ROOT_OF_2, note_index_value)
    return freq

def get_all_notes_freq():
    ordered_note_freq = []
    ordered_notes = ["C",
                     "C#",
                     "D",
                     "D#",
                     "E",
                     "F",
                     "F#",
                     "G",
                     "G#",
                     "A",
                     "A#",
                     "B"]
    for octave_index in range(2, 7):
        base_note  = "A" + str(octave_index)
        # note_index = 0  # C2
        # note_index = 12  # C3
        for note_index in range(0, 12):
            note_freq = freq_for_note(base_note, note_index)
            note_name = ordered_notes[note_index] + "_" + str(octave_index)
            ordered_note_freq.append((note_name, note_freq))
    return ordered_note_freq

def find_nearest_note(ordered_note_freq, freq):
    final_note_name = 'note_not_found'
    last_dist = 1_000_000.0
    for note_name, note_freq in ordered_note_freq:
        curr_dist = abs(note_freq - freq)
        if curr_dist < last_dist:
            last_dist = curr_dist
            final_note_name = note_name
        elif curr_dist > last_dist:
            break
    return final_note_name

# 기타 조에 대한 딕셔너리 생성
def get_all_keys_note():
    keys_freq = {
        "A": ['A_2', 'E_3', 'E_4', 'A_3'],
        "B": ['F#_2', 'B_2', 'F#_3', 'F#_4'],
        "C": ['C_3', 'E_3', 'C_4', 'E_4'],
        "D": ['D_3', 'A_3'],
        "E": ['E_2', 'B_2', 'B_3', 'E_4', 'E_3'],
        "F": ['F_2', 'C_3', 'C_4', 'F_4', 'G_2'],
        "G": ['G_2', 'B_2', 'D_3', 'G_3', 'B_3']
    }
    return keys_freq

# 기타 코드에 대한 딕셔너리 생성
def get_all_guitar_chords_notes():
    guitar_chords_with_common_notes = {
        "A": {
            "chords": ["A", "Am", "A7"],
            "identifying_notes": {
                "A": ["A_3", "C#_4"],
                "Am": ["A_3", "C_4"],
                "A7": ["G_3", "C#_4", "C#_2"]
            }
        },
        "B": {
            "chords": ["B", "Bm", "B7"],
            "identifying_notes": {
                "B": ["B_3", "D#_4"],
                "Bm": ["B_3", "D_4"],
                "B7": ["A_3", "D#_4"]
            }
        },
        "C": {
            "chords": ["C", "C7"],
            "identifying_notes": {
                "C": ["G_3"],
                "C7": ["A#_3"]
            }
        },
        "D": {
            "chords": ["D", "Dm", "D7"],
            "identifying_notes": {
                "D": ["D_4", "F#_4"],
                "Dm": ["D_4", "F_4"],
                "D7": ["C_4", "F#_4"]
            }
        },
        "E": {
            "chords": ["E", "Em", "E7"],
            "identifying_notes": {
                "E": ["E_3", "G#_3", "A#_2"],
                "Em": ["E_3", "G_3"],
                "E7": ["D_3", "G#_3", "G#_2", "D#_2"]
            }
        },
        "F": {
            "chords": ["F", "Fm", "F7"],
            "identifying_notes": {
                "F": ["F_3", "A_3"],
                "Fm": ["F_3", "G#_3"],
                "F7": ["D#_3", "A_3", "D_2", "F#_2", "C#_3", "G_2"]
            }
        },
        "G": {
            "chords": ["G", "G7"],
            "identifying_notes": {
                "G": ["G_4"],
                "G7": ["F_4"]
            }
        },
    }
    return guitar_chords_with_common_notes

# 기타 unique notes 생성
def get_unique_key():
    unique_notes = ['A_2', 'E_3', 'E_4',
                    'F#2', 'B_2', 'F#_3', 'F#_4',
                    'C_3', 'C_4',
                    'D_3', 'A_3', 'E_2',
                    'B_3', 'F_2', 'F_4',
                    'G_2', 'D_3', 'G_3']
    return unique_notes

# 기타 코드를 숫자와 매칭
def chord_to_number(chord_name):
    # 각 코드에 숫자를 부여하는 딕셔너리
    chords_numbers = {
        "A": 1, "Am": 2, "A7": 3,
        "B": 4, "Bm": 5, "B7": 6,
        "C": 7, "C7": 8,
        "D": 9, "Dm": 10, "D7": 11,
        "E": 12, "Em": 13, "E7": 14,
        "F": 15, "Fm": 16, "F7": 17,
        "G": 18, "G7": 19,
    }

    # 입력된 코드 이름에 해당하는 숫자를 반환
    return chords_numbers.get(chord_name, 'null')

# chunk별 value 기준 상위 top_n개 tuple(freq, value) 뽑기
def get_top_values(values, top_n):
    top_values = sorted(values, key=lambda x: x[1], reverse=True)[:top_n]
    return top_values

# 원하는 chunk별 value 기준 상위 top_n개 {chunk_num:(freq, value, note_name), ...} 뽑기
def get_chunks_results(all_top_results, target_chunk_nums, top_n):
    chunk_results = {}

    for chunk_num, freq, value, note_name in all_top_results:
        if chunk_num in target_chunk_nums:
            if chunk_num not in chunk_results:
                chunk_results[chunk_num] = []
            chunk_results[chunk_num].append((freq, value, note_name))

    # 각 chunk 별로 저장된 결과를 value 기준으로 내림차순 정렬하고 상위 n개만 선택
    for chunk_num in chunk_results:
        chunk_results[chunk_num] = sorted(chunk_results[chunk_num], key=lambda x: x[1], reverse=True)[:top_n]

    return chunk_results

# 기타 조 판단에 해당하는 음 저장하기
def find_unique_notes(ordered_note_freq, top_freqs, unique_notes):
    found_unique_notes = []

    # 주어진 상위 주파수들 중 unique_notes에 해당하는 음 찾기
    for freq in top_freqs:
        note_name = find_nearest_note(ordered_note_freq, freq[0])  # 주파수로부터 가장 가까운 음 찾기
        if note_name in unique_notes and note_name not in found_unique_notes:  # unique_notes 목록에 해당하는지 확인
            found_unique_notes.append(note_name)

    return found_unique_notes

# 기타 조 추정
def find_nearest_key(found_unique_notes, keys_freq):
    # found_notes가 비어있으면, 'null' 반환
    if not found_unique_notes:
        return 'null'

    # 각 조와 found_notes 간의 일치도 계산
    best_match = None
    best_match_score = -1  # 일치하는 음의 개수를 저장할 변수
    best_match_index = None  # found_notes에서 match된 최소 인덱스를 저장할 변수

    for key, notes in keys_freq.items():
        match_score = sum(note in found_unique_notes for note in notes)  # found_notes에 포함된 음의 개수를 계산
        # print("match_score", match_score)
        current_key_min_index = len(found_unique_notes)  # 현재 키에 대한 최소 인덱스 초기화
        # print("index ", current_key_min_index)

        for note in notes:
            if note in found_unique_notes:
                match_score += 1
                index = found_unique_notes.index(note)  # 현재 노트의 found_notes에서의 인덱스
                current_key_min_index = min(current_key_min_index, index)

        # 더 높은 match_score를 가진 조를 찾거나, 동일한 match_score이지만 더 낮은 인덱스를 가진 조를 찾는다
        if match_score > best_match_score or (match_score == best_match_score and current_key_min_index < best_match_index):
            best_match = key
            best_match_score = match_score
            best_match_index = current_key_min_index

    return best_match  # 가장 일치율이 높은 조 반환

# 기타 조 최종적으로 확정
def decide_majority_key(target_chunk_nums, all_keys):
    # target_chunk_nums에 해당하는 조들을 담을 리스트 초기화
    target_keys = []

    # all_keys에서 target_chunk_nums에 해당하는 조를 찾아 target_keys에 추가
    for chunk_num, key in all_keys:
        if chunk_num in target_chunk_nums:
            target_keys.append(key)

    # 다수결로 조를 결정하기 위해 각 조의 빈도수를 계산
    key_count = {}
    for key in target_keys:
        if key in key_count:
            key_count[key] += 1
        else:
            key_count[key] = 1

    # 가장 많이 나온 조를 결정 (동률일 경우 list로 반환될 수 있음)
    majority_key = [k for k, v in key_count.items() if v == max(key_count.values())]

    # 다수결의 결과가 하나의 조로 결정되면 그 조를 반환, 아니면 리스트 전체 반환
    return majority_key[0] if len(majority_key) == 1 else 'null'

# 기타 코드 확정 for chunk 1개
def find_matching_chord_for_a_chunk(final_key, chunks_results, all_guitar_chords_freq):
    if final_key == 'null':
        return 'null'

    # 결정된 조에 해당하는 코드 후보군과 식별음 가져오기
    chords = all_guitar_chords_freq[final_key]["chords"]
    identifying_notes = all_guitar_chords_freq[final_key]["identifying_notes"]

    # 각 코드별 일치하는 식별음 수 계산
    chord_matching_scores = {chord: 0 for chord in chords}

    for chunk_number, results in chunks_results.items():
        note_values = {}  # 청크별 노트 value 저장
        for freq, value, note_name in results:
            note_values[note_name] = value

        # 각 청크에서 식별음의 value를 내림차순으로 정렬
        sorted_notes_by_value = sorted(note_values.items(), key=lambda x: x[1], reverse=True)

        # 식별음 순위에 따른 점수 부여 로직
        for rank, (note_name, _) in enumerate(sorted_notes_by_value):
            for chord in chords:
                if note_name in identifying_notes[chord]:
                    # 순위가 높을수록 더 큰 점수를 부여합니다. 예: 1등은 len(results) 점, 2등은 len(results)-1 점...
                    chord_matching_scores[chord] += (len(results) - rank)

    print(chord_matching_scores)

    # 가장 높은 점수를 가진 코드 결정
    best_chord = max(chord_matching_scores, key=chord_matching_scores.get)
    return best_chord

# 기타 코드 확정을 위한 점수 list
def find_chord_matching_scores(final_key, chunks_results, all_guitar_chords_freq):
    if final_key == 'null':
        return 'null'

    # 결정된 조에 해당하는 코드 후보군과 식별음 가져오기
    chords = all_guitar_chords_freq[final_key]["chords"]
    identifying_notes = all_guitar_chords_freq[final_key]["identifying_notes"]

    # 각 코드별 일치하는 식별음 수 계산
    chord_matching_scores = {chord: 0 for chord in chords}

    for chunk_number, results in chunks_results.items():
        note_values = {}  # 청크별 노트 value 저장
        for freq, value, note_name in results:
            note_values[note_name] = value

        # 각 청크에서 식별음의 value를 내림차순으로 정렬
        sorted_notes_by_value = sorted(note_values.items(), key=lambda x: x[1], reverse=True)

        # 식별음 순위에 따른 점수 부여 로직
        for rank, (note_name, _) in enumerate(sorted_notes_by_value):
            for chord in chords:
                if note_name in identifying_notes[chord]:
                    # 순위가 높을수록 더 큰 점수를 부여합니다. 예: 1등은 len(results) 점, 2등은 len(results)-1 점...
                    chord_matching_scores[chord] += (len(results) - rank)

    print(chord_matching_scores)
    return chord_matching_scores

# 기타 코드 점수 list를 통한 기타 코드 확정
def find_matching_chord(chord_matching_scores_list):
    best_chord_list = []
    chord_frequency = {}  # 이전에 선택된 코드의 빈도를 저장할 딕셔너리

    for scores in chord_matching_scores_list:
        if not isinstance(scores, dict):
            best_chord_list.append('null')
            continue  # 다음 scores로 넘어감

        max_score = max(scores.values())  # 현재 딕셔너리에서 가장 높은 점수를 찾음
        candidates = [chord for chord, score in scores.items() if score == max_score]  # 동점인 코드를 모두 찾음

        if len(candidates) > 1:  # 동점인 코드가 여러 개인 경우
            # 이전에 가장 많이 선택된 코드를 찾거나, 동점인 경우 첫 번째 코드를 선택
            best_chord = sorted(candidates, key=lambda x: chord_frequency.get(x, 0), reverse=True)[0]
        else:
            best_chord = candidates[0]  # 동점인 코드가 하나만 있는 경우, 그 코드를 선택

        # 선택된 코드의 빈도를 업데이트
        if best_chord in chord_frequency:
            chord_frequency[best_chord] += 1
        else:
            chord_frequency[best_chord] = 1

        best_chord_list.append(best_chord)

    return best_chord_list

# 기타 박자 추정 - 앞뒤 chunk보다 중간 chunk가 큼
def find_chunks_with_peak_values(all_values):
    peak_chunks = []

    # 리스트의 길이
    length = len(all_values)

    for i in range(length):
        # 현재 chunk의 value
        current_value = all_values[i][1]

        # 이전 chunk의 value
        if i > 0:
            prev_value = all_values[i-1][1]
        else:
            prev_value = float('-inf')  # 첫 번째 요소의 경우, 이전 값이 없으므로 -무한대로 설정

        # 다음 chunk의 value
        if i < length - 1:
            next_value = all_values[i+1][1]
        else:
            next_value = float('-inf')  # 마지막 요소의 경우, 다음 값이 없으므로 -무한대로 설정

        # 현재 value가 이전과 다음의 value보다 큰 경우
        if current_value > prev_value and current_value > next_value:
            peak_chunks.append(all_values[i])

    return peak_chunks


# 증가하기 시작한 부분부터 max value까지의 차 구하기
def calculate_increase_differences(sorted_peak_chunks, all_values):
    differences = []

    for chunk_num in sorted_peak_chunks:
        # 현재 청크 번호의 값 찾기
        current_value = next(value for c_num, value in all_values if c_num == chunk_num)

        # 현재 청크 이전까지의 값들 중에서 증가 추세를 확인
        previous_values = [value for c_num, value in all_values if c_num < chunk_num]

        # 증가 추세 시작 지점 찾기
        start_increase_value = None
        for i in range(len(previous_values)-1, 0, -1):
            # 뒤에서부터 확인하여 현재 값이 이전 값보다 작으면 증가 추세 시작점으로 판단
            if previous_values[i] < previous_values[i-1]:
                start_increase_value = previous_values[i]
                break

        if start_increase_value is not None:
            # 차이 계산 후 추가
            difference = current_value - start_increase_value
            differences.append(difference)
        else:
            # 만약 모든 이전 값들이 증가 추세에 있었다면, 가장 처음 값을 증가 시작점으로 간주
            start_increase_value = previous_values[0]
            difference = current_value - start_increase_value
            differences.append(difference)

    return differences

# 기타 박자 여음 처리
def filter_euphony(differences):
    filtered_differences = []
    euphony_indices = []  # 여음으로 판단된 원소의 인덱스 저장

    for i in range(len(differences) - 1):
        current_diff = differences[i]
        next_diff = differences[i + 1]

        # 현재 원소가 다음 원소보다 크고, 그 차이가 현재 원소의 5%보다 작은 경우 여음으로 판단
        if current_diff > next_diff and next_diff < current_diff * 0.033:
            euphony_indices.append(i+1)  # 다음 원소(여음으로 판단된 원소) 인덱스 저장

    # 여음으로 판정된 원소를 제외하고 반환 목록에 추가
    for i, diff in enumerate(differences):
        if i not in euphony_indices:
            filtered_differences.append(diff)

    return filtered_differences


def PitchSpectralHps(X, freq_buckets, f_s, buffer_rms):

    """
    NOTE: This function is from the book Audio Content Analysis repository
    https://www.audiocontentanalysis.org/code/pitch-tracking/hps-2/
    The license is MIT Open Source License.
    And I have modified it. Go to the link to see the original.

    computes the maximum of the Harmonic Product Spectrum

    Args:
        X: spectrogram (dimension FFTLength X Observations)
        f_s: sample rate of audio data

    Returns:
        f HPS maximum location (in Hz)
    """
    # print("fft_res, HPS 들어간 후", "(",X.shape, ")",X)

    # initialize
    iOrder = 4
    f_min = 65.41   # C2      300
    # f = np.zeros(X.shape[1])
    f = np.zeros(len(X))

    iLen = int((X.shape[0] - 1) / iOrder)
    afHps = X[np.arange(0, iLen)]
    k_min = int(round(f_min / f_s * 2 * (X.shape[0] - 1)))

    # compute the HPS
    for j in range(1, iOrder):
        X_d = X[::(j + 1)]
        afHps *= X_d[np.arange(0, iLen)]
        # print("afHps ", afHps)

    ## Uncomment to show the original algorithm for a single frequency or note.
    # f = np.argmax(afHps[np.arange(k_min, afHps.shape[0])], axis=0)
    ## find max index and convert to Hz
    # freq_out = (f + k_min) / (X.shape[0] - 1) * f_s / 2

    note_threshold = note_threshold_scaled_by_RMS(buffer_rms)

    all_freq = np.argwhere(afHps[np.arange(k_min, afHps.shape[0])] > note_threshold)
    # find max index and convert to Hz
    freqs_out = (all_freq + k_min) / (X.shape[0] - 1) * f_s / 2


    x = afHps[np.arange(k_min, afHps.shape[0])]
    freq_indexes_out = np.where( x > note_threshold)
    freq_values_out = x[freq_indexes_out]

    # print("\n##### x: " + str(x))
    # print("\n##### freq_values_out: " + str(freq_values_out))

    max_value = np.max(afHps[np.arange(k_min, afHps.shape[0])])
    max_index = np.argmax(afHps[np.arange(k_min, afHps.shape[0])])

    ## Uncomment to print the values: buffer_RMS, max_value, min_value
    ## and note_threshold.
    print("--------------PitchSpectralHps() 거친 후----------------")
    print("buffer_rms: " + to_str_f4(buffer_rms) )
    print("max_value : " + to_str_f(max_value) + "  max_index : " + to_str_f(max_index) )
    print("note_threshold : " + to_str_f(note_threshold) )

    ## Uncomment to show the graph of the result of the
    ## Harmonic Product Spectrum.
    # fig, ax = plt.subplots()
    # yr_tmp = afHps[np.arange(k_min, afHps.shape[0])]
    # xr_tmp = (np.arange(k_min, afHps.shape[0]) + k_min) / (X.shape[0] - 1) * f_s / 2
    # ax.plot(xr_tmp, yr_tmp)
    # plt.show()

    # Turns 2 level list into a one level list.
    freqs_out_tmp = []
    for freq, value  in zip(freqs_out, freq_values_out):
        freqs_out_tmp.append((freq[0], value))

    return freqs_out_tmp

def note_threshold_scaled_by_RMS(buffer_rms):
    note_threshold = 1000.0 * (4 / 0.090) * buffer_rms
    return note_threshold

def normalize(arr):
    # Note: Do not use.
    # Normalize array between -1 and 1.
    # Only works if the signal is larger then the final signal and if the positive
    # value is grater in absolute value them the negative value.
    ar_res = (arr / (np.max(arr) / 2)) - 1
    return ar_res

def to_str_f(value):
    # Returns a string with a float without decimals.
    return "{0:.0f}".format(value)

def to_str_f4(value):
    # Returns a string with a float without decimals.
    return "{0:.4f}".format(value)

def visualize_all_freqs(chunk_num, all_freqs):
    plt.figure(figsize=(10, 4))
    freqs, values = zip(*all_freqs)
    plt.plot(freqs, values)
    plt.title(f'Chunk {chunk_num} - Frequency vs Value')
    plt.xlabel('Frequency (Hz)')
    plt.ylabel('Value')
    plt.grid(True)
    plt.show()

def expand_results(results):
    new_results = [results[0]]  # 첫 번째 원소는 그대로 옮김

    # 나머지 24개의 원소를 각각 3번씩 반복하여 추가
    for element in results[1:]:
        new_results.extend([element] * 3)

    return new_results


def main(wave_bytes, rms_list):
    print("\nPolyphonic note detector\n")

    unique_notes = get_unique_key()
    keys_note = get_all_keys_note()
    guitar_chords_notes = get_all_guitar_chords_notes()
    ordered_note_freq = get_all_notes_freq()

    sample_rate_file, input_buffer = read_wav_file(wave_bytes)
    buffer_chunks = divide_buffer_into_non_overlapping_chunks(input_buffer, fft_len)

    rms_list = rms_list.toArray()  # android에서 받아온 시간별 dB 결과
    rms_list = [(pair.getFirst(), pair.getSecond()) for pair in rms_list]

    all_top_results = [] # [(chunk_num, freq, value, note_name), ...]
    all_values = []      # chunk별 상위 1개의 value 모음, [(chunk_num, value), ....]
    # all_freq_num = [0, ]    # chunk별 추출된 freq 모음
    all_keys = []        # 추정한 조 모음, [(chunk_num, nearest_key), ...]
    all_chords = []      # 추정한 코드 모음, ['G', 'null', ...]
    chunk_num = 0
    top_n = 10

    results = []
    chunk_times = []

    # chunk 별로 HPS 수행
    for chunk in buffer_chunks[0: 60]:
        start_time = (chunk_num * len(chunk)) / sample_rate_file
        end_time = ((chunk_num + 1) * len(chunk)) / sample_rate_file
        print(f"\nChunk {chunk_num+1}, 시간: {start_time:.2f}초~{end_time:.2f}초")
        chunk_times.append((chunk_num+1, start_time))  # chunk 시작 시간 추가

        fft_freq, fft_res, fft_res_len = getFFT(chunk, len(chunk))
        fft_res = remove_dc_offset(fft_res)
        buffer_rms = np.sqrt(np.mean(chunk**2))
        all_freqs = PitchSpectralHps(fft_res, fft_freq, sample_rate_file, buffer_rms)


        ''' 코드 추정을 위한 코드 '''
        # freq 기준 상위 n개 tuple(freq, value)를 저장
        top_results = get_top_values(all_freqs, top_n)
        # top_20_results =  get_top_values(all_freqs, 20)


        ''' 박자 추정을 위한 코드 '''
        # value 기준 상위 n개 tuple(freq, value)를 저장
        top_values_result = get_top_values(all_freqs, 1)
        # chunk별 value 기준 상위 1개의 value만을 저장
        if top_values_result:
            all_values.append((chunk_num+1, int(top_values_result[0][1])))
        else:
            all_values.append((chunk_num+1, 0))


        ''' 출력을 위한 코드 '''
        # freq 기준 상위 n개 tuple(freq, value) 출력
        for freq in top_results:
            note_name = find_nearest_note(ordered_note_freq, freq[0])
            print("=> freq: " + to_str_f(freq[0]) + " Hz  value: " + to_str_f(freq[1]) + " note_name: " + note_name)
            # all_top_result에 추가
            all_top_results.append((chunk_num+1, freq[0], freq[1], note_name))  # chunk 번호, freq, value, 음 순으로 저장

        # all_freqs(freq, value) 출력
        # for freq in all_freqs:
        #     note_name = find_nearest_note(ordered_note_freq, freq[0])
        #     print("=> freq: " + to_str_f(freq[0]) + " Hz  value: " + to_str_f(freq[1]) + " note_name: " + note_name)


        ''' 조 추정을 위한 코드 '''
        # freq 기준 상위 n개의 음 중 unique_notes에 해당하는 음을 found_unique_notes에 담기
        found_unique_notes = find_unique_notes(ordered_note_freq, top_results, unique_notes)
        print(found_unique_notes)

        # found_unique_notes의 음과 keys_note의 음을 비교하여 조 찾기
        nearest_key = find_nearest_key(found_unique_notes, keys_note)
        print(nearest_key, "조")

        # all_keys에 추정한 조 추가
        all_keys.append((chunk_num+1, nearest_key))


        ''' 코드 추정을 위한 코드 '''
        chunks_top_results = get_chunks_results(all_top_results, [chunk_num+1], top_n)  # {chunk_num:(freq, value, note_name), ...}
        final_chord = find_matching_chord_for_a_chunk(nearest_key, chunks_top_results, guitar_chords_notes)
        print(final_chord, "코드")
        all_chords.append(final_chord)
        # all_freq_num.append(len(all_freqs))

        chunk_num += 1


    print("\n---------------chunk별 조 결과----------------")
    print(all_keys)

    print("\n---------------chunk별 코드 결과----------------")
    print(all_chords)

    print("\n---------------chunk별 상위 1개 value 결과----------------")
    print(all_values)

    print("\n---------------chunk별 시작 시간----------------")
    print(chunk_times)

    print("\n---------------시간별 dB 결과----------------")
    print(rms_list)


    print("\n---------------박자 확정 과정 및 결과---------------")
    # chunk별 dB 결과
    rms_list = [((chunk[0] - 2000) / 200 + 1, chunk[1]) for chunk in rms_list]

    # peak_chunks 찾기
    peak_chunks = find_chunks_with_peak_values(rms_list)
    print(peak_chunks)
    # peak_chunks = [(math.floor(value), duration) for value, duration in peak_chunks] # 각 chunk의 소수 버리기

    # 박자 친 부분 확정
    peak_chunk_nums_double = [chunk[0] for chunk in peak_chunks]
    print(peak_chunk_nums_double)
    previous_peak_chunk_nums_double = [chunk[0]-1 for chunk in peak_chunks]
    print(previous_peak_chunk_nums_double)
    previous_peak_chunk_nums = [int(num) for num in previous_peak_chunk_nums_double]
    print(previous_peak_chunk_nums)

    # 각 숫자로부터 뒤로 3개의 숫자를 포함하는 이중리스트 생성
    extended_peak_chunks = []
    # is_it_onetwothree = 1  # peack_chunk_nums가 -1이 나왔을때 -1, 0이 나왔을때 0, 안나왔을때 1로 설정
    # -1이나 0이 있을 경우, [1, 2, 3]을 추가
    if -1 in previous_peak_chunk_nums:
        extended_peak_chunks.append([1, 2, 3])
        previous_peak_chunk_nums.remove(-1)  # 처리된 -1 제거
        # is_it_onetwothree = -1
    if 0 in previous_peak_chunk_nums:
        extended_peak_chunks.append([1, 2, 3])
        previous_peak_chunk_nums.remove(0)  # 처리된 0 제거
        # is_it_onetwothree = 0

    # 나머지 숫자들에 대해 각 숫자로부터 뒤로 3개의 숫자를 포함하는 리스트 추가
    for num in previous_peak_chunk_nums:
        extended_peak_chunks.append([num + i for i in range(3)])

    # 중복되는 [1,2,3] 있으면 하나만 남겨두고 제거
    # unique_extended_peak_chunks = []
    # for chunk in extended_peak_chunks:
    #     if chunk not in unique_extended_peak_chunks:
    #         unique_extended_peak_chunks.append(chunk)
    # print(unique_extended_peak_chunks)

    # 중복되는 [1,2,3] 있으면 하나만 남겨두고 제거
    unique_extended_peak_chunks = []
    for chunk in extended_peak_chunks:
        int_chunk = [int(number) for number in chunk]  # 소수점을 제거하고 정수로 변환
        if int_chunk not in unique_extended_peak_chunks:  # 중복 제거
            unique_extended_peak_chunks.append(int_chunk)
    print(unique_extended_peak_chunks)


    print("\n---------------코드 확정 과정 및 결과----------------")
    final_chord_list = []
    final_chord_matching_scores = []
    final_chord_matching_scores_list = []
    for current_target_chunk_nums in unique_extended_peak_chunks:
        # 1. 해당하는 chunk의 조 결과를 all_keys를 통해 확인하고, 해당하는 chunk의 조를 최종적으로 확정

        # if is_it_onetwothree == -1:
        #     chunk_target_chunk_nums = [0, 1, 2]
        # elif is_it_onetwothree == 0:
        #     chunk_target_chunk_nums = [0, 1, 2]

        # print("current_target_chunk_nums : ", current_target_chunk_nums)
        ## 원하는 chunk별, value 기준 상위 n개 {chunk_num:(freq, value, note_name), ...} 출력
        chunks_top_results = get_chunks_results(all_top_results, current_target_chunk_nums, top_n)
        # for chunk_number, results in chunks_top_results.items():
        #     for freq, value, note_name in results:
        #         print(f"=> freq: {to_str_f(freq)} Hz  value: {to_str_f(value)} note_name: {note_name} chunk num: {chunk_number}")

        ## target_chunk별 정해진 조 출력
        # for chunk_num in current_target_chunk_nums:
        #     for chunk_key in all_keys:
        #         if chunk_key[0] == chunk_num:
        #             print(f"Chunk {chunk_num}의 조: {chunk_key[1]}")

        ## target_chunk를 통해 최종적으로 조 확정
        final_key = decide_majority_key(current_target_chunk_nums, all_keys)
        # print(f"최종 결정된 조: {final_key}")


        # 2. 해당하는 조의 코드에서 공통되는 음을 제외한 음들이, chunks_top_results 음에 많이 있는지 비교 확인한 후 코드 확정
        final_chord_matching_scores = find_chord_matching_scores(final_key, chunks_top_results, guitar_chords_notes)
        final_chord_matching_scores_list.append(final_chord_matching_scores)
        #### final_chord = find_matching_chord(final_key, chunks_top_results, guitar_chords_notes)
        # print(f"최종 결정된 코드: {final_chord}")
        #### final_chord_list.append(final_chord)

    print(final_chord_matching_scores_list)
    final_chord_list = find_matching_chord(final_chord_matching_scores_list)
    print(final_chord_list)
    numbered_final_chord_list = [chord_to_number(chord) for chord in final_chord_list]
    print(numbered_final_chord_list)

    # 'null'이 아닌 첫 번째 값 찾기
    first_non_null_value = None
    for chord in numbered_final_chord_list:
        if chord != 'null':
            first_non_null_value = chord
            break

    # 'null' 값을 찾아서 'null'이 아닌 첫 번째 값으로 대체
    numbered_final_chord_list = [first_non_null_value if chord == 'null' else chord for chord in numbered_final_chord_list]
    print(numbered_final_chord_list)

    print("\n---------------박자 여음 처리 이전, 코드 및 박자 결과----------------")
    # 박자 체크
    results = [1 if i in previous_peak_chunk_nums else 0 for i in range(0, chunk_num+1)]
    print(results)
    # 코드 체크
    chord_pointer = 0
    for i, val in enumerate(results):
        if val == 1:
            if numbered_final_chord_list[chord_pointer] == 'null':
                results[i] = 0
            else:
                results[i] = numbered_final_chord_list[chord_pointer]
            chord_pointer = chord_pointer + 1
    # 코드와 박자
    print(results)


    print("\n---------------박자 여음 처리 과정 및 결과----------------")
    print(rms_list)

    # 박자 친 부분, 즉 가운데 chunk
    peak_chunk_nums = [i+1 for i, val in enumerate(results) if val != 0]
    print(peak_chunk_nums)

    # results에서 살아남은 박자가 몇 chunk(double)이었는지 알아내야함
    # 그리고 이를 calculate_increase_differneces에 peack_chunk_nums 대신 넣어야함
    # print(peak_chunk_nums_double)
    # survived_peak_chunk_nums_double = [peak_chunk_nums_double[i] for i, val in enumerate(results) if val != 0]
    survived_peak_chunk_nums_double = []
    for i, chord_number in enumerate(numbered_final_chord_list):
        if chord_number != 'null':  # 코드 결과가 'null'이 아니면,
            # 해당 인덱스에 해당하는 peak_chunk_nums_double 값을 추가합니다.
            survived_peak_chunk_nums_double.append(peak_chunk_nums_double[i])
    print(survived_peak_chunk_nums_double)

    # 증가하기 시작한 부분의 value와 max value까지의 차 저장
    differences = calculate_increase_differences(survived_peak_chunk_nums_double, rms_list)
    print(differences)

    # 차를 봤을때 앞에 차보다 많이 작으면 여음으로 판정하여 differneces에서 차에 대한걸 삭제함
    filtered_differences = filter_euphony(differences)
    print(filtered_differences)


    print("\n---------------박자 여음 처리 이후, 코드 및 박자 결과----------------")
    print(all_values)
    print(rms_list)

    for i, difference in enumerate(differences):
        if difference not in filtered_differences:
            results[peak_chunk_nums[i]-1] = 0
    print("원본 : ", results)
    print(len(results))

    # 인덱스 15부터 시작하여, 0이 아닌 값들을 한 칸씩 앞으로 당기기
    # i = 15
    # while i < len(results) - 1:
    #     # 현재 인덱스가 0이 아니고, 다음 인덱스도 0이 아닌 경우
    #     if results[i] != 0 and results[i + 1] != 0:
    #         # 다음 인덱스부터 시작하여 0이 아닌 값을 한 칸씩 앞으로 당김
    #         j = i + 1
    #         while j < len(results) - 1:
    #             results[j] = results[j + 1]
    #             j += 1
    #         results[j] = 0  # 마지막에 도달했을 때, 마지막 값을 0으로 설정
    #     # 현재 인덱스가 0이고, 다음 인덱스가 0이 아닌 경우
    #     elif results[i] == 0 and results[i + 1] != 0:
    #         results[i] = results[i + 1]
    #         i += 1  # 다음 위치로 이동
    #         # 당긴 후의 나머지 값을 처리
    #         while i < len(results) - 1:
    #             results[i] = results[i + 1]
    #             i += 1
    #         results[i] = 0  # 마지막 값을 0으로 설정
    #         break  # 처리 완료 후 반복문 탈출
    #     else:
    #         i += 1  # 다음 인덱스로 이동
    # print("앞댕 : ", results)

    # 인덱스 27 이후의 모든 요소를 0으로 설정
    if len(results) > 27:
        for i in range(27, len(results)):
            results[i] = 0
    print("0설 : ",results)
    print(len(results))

    # 인덱스 1과 2의 요소를 삭제
    del results[1]  # 인덱스 1의 요소 삭제
    del results[1]  # 원래 인덱스 2의 요소가 삭제되면서 인덱스가 한 칸씩 당겨지므로 다시 인덱스 1의 요소를 삭제
    print("삭제 : ",results)
    print(len(results))

    # 원소 1개를 3개로 늘려서 list 반환
    results = expand_results(results)
    print(results)
    print(len(results))
    return results

if __name__ == "__main__":
    main()