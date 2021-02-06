class AlarmNotificationMeta {
  final String activityClassLaunchedByIntent;
  final String iconDrawableResourceName;
  final String contentTitle;
  final String contentText;
  final String subText;
  final int color;

  AlarmNotificationMeta(
      this.activityClassLaunchedByIntent, this.iconDrawableResourceName,
      {this.contentTitle, this.contentText, this.subText, this.color});

  Map<String, dynamic> toMap() => {
        'activityClassLaunchedByIntent': activityClassLaunchedByIntent,
        'iconDrawableResourceName': iconDrawableResourceName,
        'contentTitle': contentTitle,
        'contentText': contentText,
        'subText': subText,
        'color': color,
      };
}
