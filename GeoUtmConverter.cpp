#include "GeoUtmConverter.h"
#include <cmath>
#include <QDebug>
// ======================== 工具函数 ========================

double GeoUtmConverter::deg2rad(double d)
{
    return d * M_PI / 180.0;
}

double GeoUtmConverter::rad2deg(double r)
{
    return r * 180.0 / M_PI;
}

// XE 编码 / 解码
double GeoUtmConverter::makeXE(int zone, double easting)
{
    return zone * 1e6 + easting;
}

int GeoUtmConverter::extractZone(double XE)
{
    return static_cast<int>(XE / 1e6);
}

double GeoUtmConverter::extractEasting(double XE)
{
    int zone = extractZone(XE);
    return XE - zone * 1e6;
}

// ======================== 构造函数 ========================

// 默认：WGS-84 椭球
GeoUtmConverter::GeoUtmConverter()
{
    m_a  = 6378137.0;                      // 长半轴
    m_f  = 1.0 / 298.257223563;           // 扁率
    m_e2 = m_f * (2.0 - m_f);             // 第一偏心率平方
    m_k0 = 0.9996;                        // UTM 尺度因子
}

// 自定义椭球
GeoUtmConverter::GeoUtmConverter(double a, double f, double k0)
{
    m_a  = a;
    m_f  = f;
    m_e2 = m_f * (2.0 - m_f);
    m_k0 = k0;
}

// ======================== UTM -> Geo ========================

GeoUtmConverter::GeoCoord
GeoUtmConverter::utmToGeo(const UtmCoord& utm) const
{
    GeoCoord geo{};

    // 从 XE 中解出 zone 和 easting
    int    zone    = extractZone(utm.XE);
    double easting = extractEasting(utm.XE);

    // 中央经线
    double lon0Deg = (zone - 1) * 6.0 - 180.0 + 3.0;
    double lon0 = deg2rad(lon0Deg);

    // 去掉假东、假北
    const double FALSE_EASTING  = 500000.0;
    const double FALSE_NORTHING_SOUTH = 10000000.0;

    double x = easting - FALSE_EASTING;
    double y = utm.northing;
    if (!utm.northHemisphere)
        y -= FALSE_NORTHING_SOUTH;

    // 计算子午线弧长 M 和 μ
    double M = y / m_k0;
    double e2 = m_e2;
    double e4 = e2 * e2;
    double e6 = e4 * e2;

    double mu = M / (m_a * (1.0
        - e2 / 4.0 - 3.0 * e4 / 64.0 - 5.0 * e6 / 256.0));

    // 展开系数 e1
    double e1 = (1.0 - std::sqrt(1.0 - e2)) / (1.0 + std::sqrt(1.0 - e2));

    // 计算 φ1（底纬）
    double J1 = (3.0 * e1 / 2.0 - 27.0 * std::pow(e1, 3) / 32.0);
    double J2 = (21.0 * e1 * e1 / 16.0 - 55.0 * std::pow(e1, 4) / 32.0);
    double J3 = (151.0 * std::pow(e1, 3) / 96.0);
    double J4 = (1097.0 * std::pow(e1, 4) / 512.0);

    double phi1 = mu
        + J1 * std::sin(2.0 * mu)
        + J2 * std::sin(4.0 * mu)
        + J3 * std::sin(6.0 * mu)
        + J4 * std::sin(8.0 * mu);

    double sinPhi1 = std::sin(phi1);
    double cosPhi1 = std::cos(phi1);
    double tanPhi1 = std::tan(phi1);

    double ePrime2 = m_e2 / (1.0 - m_e2);
    double N1 = m_a / std::sqrt(1.0 - e2 * sinPhi1 * sinPhi1);
    double R1 = m_a * (1.0 - e2) / std::pow(1.0 - e2 * sinPhi1 * sinPhi1, 1.5);
    double T1 = tanPhi1 * tanPhi1;
    double C1 = ePrime2 * cosPhi1 * cosPhi1;
    double D  = x / (N1 * m_k0);

    // 反算纬度
    double lat = phi1 - (N1 * tanPhi1 / R1) * (
        D * D / 2.0
        - (5.0 + 3.0 * T1 + 10.0 * C1 - 4.0 * C1 * C1 - 9.0 * ePrime2) * std::pow(D, 4) / 24.0
        + (61.0 + 90.0 * T1 + 298.0 * C1 + 45.0 * T1 * T1 - 252.0 * ePrime2 - 3.0 * C1 * C1) * std::pow(D, 6) / 720.0
    );

    // 反算经度
    double lon = lon0 + (
        D
        - (1.0 + 2.0 * T1 + C1) * std::pow(D, 3) / 6.0
        + (5.0 - 2.0 * C1 + 28.0 * T1 - 3.0 * C1 * C1 + 8.0 * ePrime2 + 24.0 * T1 * T1) * std::pow(D, 5) / 120.0
    ) / cosPhi1;

    geo.latDeg = rad2deg(lat);
    geo.lonDeg = rad2deg(lon);
    geo.height = utm.height;
    return geo;
}

// ======================== Geo -> UTM ========================
GeoUtmConverter::UtmCoord
GeoUtmConverter::geoToUtm(const GeoCoord& geo, int utmZone) const
{
    UtmCoord utm{};

    double lat = deg2rad(geo.latDeg);
    double lon = deg2rad(geo.lonDeg);

    // 如果没有提供带号，则自动计算
    if (utmZone == -1)
    {
        // 自动计算 UTM 带号（6° 带）
        utmZone = static_cast<int>(std::floor((geo.lonDeg + 180.0) / 6.0)) + 1;
    }

    // 根据指定的带号计算中央经线
    double lon0Deg = (utmZone - 1) * 6.0 - 180.0 + 3.0;
    double lon0 = deg2rad(lon0Deg);

    double ePrime2 = m_e2 / (1.0 - m_e2);

    double sinLat = std::sin(lat);
    double cosLat = std::cos(lat);
    double tanLat = std::tan(lat);

    double N = m_a / std::sqrt(1.0 - m_e2 * sinLat * sinLat);
    double T = tanLat * tanLat;
    double C = ePrime2 * cosLat * cosLat;
    double A = cosLat * (lon - lon0);

    // 子午线弧长 M
    double e2 = m_e2;
    double e4 = e2 * e2;
    double e6 = e4 * e2;
    double M = m_a * (
        (1.0 - e2 / 4.0 - 3.0 * e4 / 64.0 - 5.0 * e6 / 256.0) * lat
        - (3.0 * e2 / 8.0 + 3.0 * e4 / 32.0 + 45.0 * e6 / 1024.0) * std::sin(2.0 * lat)
        + (15.0 * e4 / 256.0 + 45.0 * e6 / 1024.0) * std::sin(4.0 * lat)
        - (35.0 * e6 / 3072.0) * std::sin(6.0 * lat)
    );

    // 东、北坐标
    double x = m_k0 * N * (A + (1.0 - T + C) * std::pow(A, 3) / 6.0
        + (5.0 - 18.0 * T + T * T + 72.0 * C - 58.0 * ePrime2) * std::pow(A, 5) / 120.0);

    double y = m_k0 * (M + N * tanLat * (A * A / 2.0
        + (5.0 - T + 9.0 * C + 4.0 * C * C) * std::pow(A, 4) / 24.0
        + (61.0 - 58.0 * T + T * T + 600.0 * C - 330.0 * ePrime2) * std::pow(A, 6) / 720.0));

    const double FALSE_EASTING  = 500000.0;
    const double FALSE_NORTHING_SOUTH = 10000000.0;

    double easting  = x + FALSE_EASTING;
    double northing = y;

    utm.northHemisphere = (geo.latDeg >= 0.0);
    if (!utm.northHemisphere)
        northing += FALSE_NORTHING_SOUTH;

    utm.XE       = makeXE(utmZone, easting);
    utm.northing = northing;
    utm.height   = geo.height;
    return utm;
}


// ======================== Geo -> ECEF ========================

void GeoUtmConverter::geoToECEF(const GeoCoord& geo,
                                double& X, double& Y, double& Z) const
{
    double lat = deg2rad(geo.latDeg);
    double lon = deg2rad(geo.lonDeg);
    double h   = geo.height;

    double sinLat = std::sin(lat);
    double cosLat = std::cos(lat);
    double sinLon = std::sin(lon);
    double cosLon = std::cos(lon);

    double N = m_a / std::sqrt(1.0 - m_e2 * sinLat * sinLat);

    X = (N + h) * cosLat * cosLon;
    Y = (N + h) * cosLat * sinLon;
    Z = (N * (1.0 - m_e2) + h) * sinLat;
}

// ======================== ECEF -> Geo ========================

GeoUtmConverter::GeoCoord
GeoUtmConverter::ecefToGeo(double X, double Y, double Z) const
{
    GeoCoord geo{};
    double b  = m_a * (1.0 - m_f);               // 短半轴
    double ep2 = (m_a * m_a - b * b) / (b * b);  // 第二偏心率平方

    double p = std::sqrt(X * X + Y * Y);
    double theta = std::atan2(Z * m_a, p * b);

    double sinTheta = std::sin(theta);
    double cosTheta = std::cos(theta);

    double lat = std::atan2(Z + ep2 * b * sinTheta * sinTheta * sinTheta,
                            p - m_e2 * m_a * cosTheta * cosTheta * cosTheta);
    double lon = std::atan2(Y, X);

    double sinLat = std::sin(lat);
    double N = m_a / std::sqrt(1.0 - m_e2 * sinLat * sinLat);
    double h = p / std::cos(lat) - N;

    geo.latDeg = rad2deg(lat);
    geo.lonDeg = rad2deg(lon);
    geo.height = h;
    return geo;
}


// ======================== 计算目标坐标（基于经纬度 + 距离/方位/高低角，弧度输入） ========================
//
// 说明：短距离（<~20 km）使用椭球小量展开：
//   dφ ≈ dN / M(φ0)
//   dλ ≈ dE / (N(φ0) cosφ0)
// 精度可达厘米级。
//
GeoUtmConverter::GeoCoord
GeoUtmConverter::computeTargetFromGeo(const GeoCoord& selfGeo,
                                      double range_m,
                                      double azimuth_rad,
                                      double elevation_rad) const
{
    GeoCoord tgt{};

    // 起点纬度、经度（rad）
    double lat0 = deg2rad(selfGeo.latDeg);
    double lon0 = deg2rad(selfGeo.lonDeg);

    double sinLat0 = std::sin(lat0);
    double cosLat0 = std::cos(lat0);

    // 椭球参数
    double e2 = m_e2;
    double a  = m_a;

    // 曲率半径
    double N = a / std::sqrt(1.0 - e2 * sinLat0 * sinLat0);
    double M = a * (1.0 - e2) / std::pow(1.0 - e2 * sinLat0 * sinLat0, 1.5);

    // 1) 拆分斜距
    double S  = range_m * std::cos(elevation_rad); // 水平距
    double dH = range_m * std::sin(elevation_rad); // 高程差

    // 2) 水平分量在真东/真北方向上的投影
    double dN = S * std::cos(azimuth_rad); // 北向
    double dE = S * std::sin(azimuth_rad); // 东向

    // 3) 小量展开（rad）
    double dLat = dN / M;
    double dLon = dE / (N * cosLat0);

    double lat1 = lat0 + dLat;
    double lon1 = lon0 + dLon;

    tgt.latDeg = rad2deg(lat1);
    tgt.lonDeg = rad2deg(lon1);
    tgt.height = selfGeo.height + dH;

    return tgt;
}

// ======================== 网格北方位角 -> 真北方位角 ========================
//
// gamma ≈ (λ - λ0) * sinφ
// az_true = az_grid + gamma
//
double GeoUtmConverter::gridAzToTrueAzRad(const UtmCoord& selfUtm,
                                          double az_grid_rad) const
{
    // 1) 己方 UTM -> 大地
    GeoCoord selfGeo = utmToGeo(selfUtm);

    double lat = deg2rad(selfGeo.latDeg);
    double lon = deg2rad(selfGeo.lonDeg);

    // 2) 所在带中央经线
    int zone = extractZone(selfUtm.XE);
    double lon0Deg = (zone - 1) * 6.0 - 180.0 + 3.0;
    double lon0 = deg2rad(lon0Deg);

    // 3) 会聚角 gamma（rad）
    double dLam = lon - lon0;
    double gamma = std::atan(std::tan(dLam) * std::sin(lat));

    // 4) Grid 北方位角 -> 真北方位角
    double az_true = az_grid_rad + gamma;

    // 归一化到 [0, 2π)
    const double TWO_PI = 2.0 * M_PI;
    while (az_true < 0.0)     az_true += TWO_PI;
    while (az_true >= TWO_PI) az_true -= TWO_PI;

    return az_true;
}

GeoUtmConverter::UtmCoord
GeoUtmConverter::checkAndCorrectCrossedZone(const UtmCoord& selfUtm) const
{
    // 1. 使用 UTM 坐标转换为经纬度
    GeoCoord geo = utmToGeo(selfUtm);

    // 2. 计算该经纬度所在的 UTM 带号
    int targetZone = static_cast<int>(std::floor((geo.lonDeg + 180.0) / 6.0)) + 1;

    // 3. 判断是否跨带
    int currentZone = GeoUtmConverter::extractZone(selfUtm.XE);
    if (currentZone != targetZone)
    {
        // 目标 UTM 坐标和新带号
        qDebug() << "Crossed UTM zone! From Zone " << currentZone << " to Zone " << targetZone ;

        // 根据新的经纬度计算 UTM 坐标
        UtmCoord newUtm = geoToUtm(geo);

        // 新的 UTM 坐标
        qDebug() << "New UTM Coordinates: " << "Easting: " << GeoUtmConverter::extractEasting(newUtm.XE)
                  << ", Northing: " << newUtm.northing << ", Height: " << newUtm.height ;

        return newUtm;
    }

    // 如果没有跨带，返回原 UTM 坐标
    return selfUtm;
}

