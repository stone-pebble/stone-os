.class public Lcom/android/systemui/stone/StonePanelController;
.super Ljava/lang/Object;
.source "StonePanelController.java"

# static fields
.field private static final TAG:Ljava/lang/String; = "StoneOS"

# instance fields
.field private mContext:Landroid/content/Context;
.field private mIsShowing:Z

# direct methods
.method public constructor <init>(Landroid/content/Context;)V
    .locals 1

    invoke-direct {p0}, Ljava/lang/Object;-><init>()V
    
    iput-object p1, p0, Lcom/android/systemui/stone/StonePanelController;->mContext:Landroid/content/Context;
    
    const/4 v0, 0x0
    iput-boolean v0, p0, Lcom/android/systemui/stone/StonePanelController;->mIsShowing:Z
    
    # Log initialization
    const-string v0, "StoneOS"
    const-string v1, "Stone Panel Controller Created"
    invoke-static {v0, v1}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I
    
    return-void
.end method

.method public showStoneIcon()V
    .locals 2

    const-string v0, "StoneOS"
    const-string v1, "Stone Icon Display Requested"
    invoke-static {v0, v1}, Landroid/util/Log;->i(Ljava/lang/String;Ljava/lang/String;)I

    const/4 v0, 0x1
    iput-boolean v0, p0, Lcom/android/systemui/stone/StonePanelController;->mIsShowing:Z

    return-void
.end method