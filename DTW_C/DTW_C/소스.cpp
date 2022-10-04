#include <iostream>
#include <fstream>
#include <sstream>
#include <vector>
#include <string>
#include <cmath>
#include <algorithm>
#include <limits>
#include <list>

using namespace std;

std::vector<std::string> split(std::string str, char del) {

    std::vector<std::string> rst;
    std::stringstream ss(str);
    std::string tmp;

    while (std::getline(ss, tmp, del))
        rst.push_back(tmp);

    return rst;
}

std::vector<std::vector<std::string>> readRef3DPosition(std::string file_path) {

    /* txt file */
    std::ifstream ifile(file_path);
    std::vector<std::vector<std::string>> data;

    if (ifile) {
        std::string line;
        std::vector<std::string> rst;

        while (std::getline(ifile, line)) {
            std::cout << line << std::endl;
            rst = split(line, '\t');
            data.push_back(rst);
        }

        ifile.close();
        return data;
    }
}

float Dist(float x0, float y0) {
    return abs(x0 - y0);
}

int DTW(int x[], int y[], int warp = 1, float w = numeric_limits<int>::max(), float s = 1.0) {
    /*
    Computes Dynamic Time Warping(DTW) of two sequences.

    : param array x : N1 * M array
    : param array y : N2 * M array
    : param func dist : distance used as cost measure
    : param int warp : how many shifts are computed.
    : param int w : window size limiting the maximal distance between indices of matched entries | i, j | .
    : param float s : weight applied on off - diagonal moves of the path.As s gets larger, the warping path is increasingly biased towards the diagonal
    Returns the minimum distance, the cost matrix, the accumulated cost matrix, and the wrap path.
    */

    //std::int16_t r = x.size();
    const int r = sizeof(x);
    const int c = sizeof(y);
    int D0[r + 1][c + 1] = { 0 };
    int D1[r][c] = { 0 };
    int C[r][c] = { 0 };

    if (not isinf(w)) {

        memset(&D0, numeric_limits<int>::max(), sizeof(D0));
        for (int i = 0;i < r + 1;i++) {
            int max_value = max(1, int(i - w));
            int min_value = min(c + 1, int(i + w + 1));

            for (int j = max_value; j < min_value; j++) {
                D0[i][j] = 0;
            }
        }
        D0[0][0] = 0;
    }
    else {
        for (int i = 1;i <= c;i++) { D0[0][i] = numeric_limits<int>::max(); }
        for (int i = 1;i <= r;i++) { D0[i][0] = numeric_limits<int>::max(); }
    }
    for (int i = 2;i < c;i++) {
        copy(&D0[i][1], &D0[i][1] + c, &D1[i][1]); //D1 = D0[1:, 1 : ]
    }

    for (int i = 0;i < r;i++) {
        for (int j = 0;j < r;j++) {
            if (isinf(w) or max(0, int(i - w)) <= j <= min(c, int(i + w))) {
                D1[i][j] = Dist(x[i], y[j]);
            }
        }
    }

    copy(&D1[0][0], &D1[0][0] + r * c, C);

    list<int> jrange;
    list<int> jrange2;
    vector<int> min_list;
    for (int i = 0;i < c;i++) {
        jrange.push_back(i);
    }

    for (int i = 0;i < r;i++) {

        int j_max = max(0, int(i - w));
        int j_min = min(c, int(i + w + 1));

        if (not isinf(w)) {
            for (int i = j_max; i < j_min; i++) {
                jrange2.push_back(i);
            }
        }
        for (int j : jrange) {
            min_list = { D0[i][j] };
            for (int k = 1;k < warp + 1;k++) {
                int i_k = min(i + k, r);
                int j_k = min(j + k, c);
                min_list.push_back(D0[i_k][j] * s);
                min_list.push_back(D0[i][j_k] * s);
            }

            int smallest = min_list[0];
            for (int i = 1;i < min_list.size();++i) { smallest = min(smallest, min_list[i]); }
        }
    }

    return D1[r][c];
}


int main() {

    std::string file_path = "C:/Users/82109/Desktop/AppStudy/DTW_C/video1.txt";

    std::vector<std::vector<std::string>> data = readRef3DPosition(file_path);
    
    for (int i = 0; i < data.size(); i++) { //data.size() -> За
        for (int j = 0; j < data[0].size(); j++) { //data[0].size() -> ї­
            cout << (*(data.begin() + i))[j];
        }
    }

	return 0;
}