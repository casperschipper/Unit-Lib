+ ScaledUserView {
	
	drawTempoGrid { |tempoMap|
		var viewRect, pixelScale;
		var barLines, firstBar;
		var top, bottom, left60, leftRounded;
		var round, width, scaleAmt;
		if( tempoMap.isNil ) {
			tempoMap = TempoMap();
		};
		pixelScale = this.pixelScale;
		viewRect = this.viewRect;
		top = viewRect.top;
		bottom = viewRect.bottom;
		width = viewRect.width.ceil;
		round = round = (width / 20).max(1).nextPowerOfTwo.asInt;
		left60 = viewRect.left.round(60);
		leftRounded = viewRect.left.round(round);
		//bnds = "000".bounds( Font( Font.defaultSansFace, 9 ) );
		//bnds.width = bnds.width + 4;
		scaleAmt = 1/this.scaleAmt.asArray;
		barLines = tempoMap.barLines( viewRect.left, viewRect.right );
		firstBar = tempoMap.barAtTime(barLines.first ? 0)[0];
		
		Pen.color = Color.gray.alpha_(0.25);
		
		if( viewRect.width < (this.view.bounds.width/100) ) {
			Pen.use({	
				Pen.width = pixelScale.x / 2;
				Pen.lineDash_( FloatArray[ 0.5, 0.5 ] );
				tempoMap.divisionLines( viewRect.left, viewRect.right, 0.25 ).do({ |i|
					Pen.line( i @ top, i @ bottom );
				});
				Pen.stroke;
			});
		};
		
		if( viewRect.width < (this.view.bounds.width/4) ) {
			Pen.width = pixelScale.x / 2;
			tempoMap.divisionLines( viewRect.left, viewRect.right ).do({ |i|
				Pen.line( i @ top, i @ bottom );
			});
			Pen.stroke;
		};
		
		if( viewRect.width < (this.view.bounds.width) ) {
			Pen.width = pixelScale.x;
			barLines.do({ |i|
				Pen.line( i @ top, i @ bottom );
			});
			Pen.stroke;
		};
		
		Pen.color = Color.white.alpha_(0.75);
		Pen.width = pixelScale.x;
		(width / 60).ceil.do({ |i|
			i = (i * 60) + left60;
			Pen.line( i @ top, i @ bottom );
		});
		Pen.stroke;
		
		(barLines ? [])[0,round..].do({ |item, i|
			Pen.use({
				var string, bnds;
				string = (firstBar + (i * round)).asString;
				bnds = string.bounds( Font( Font.defaultSansFace, 9 ) );
				bnds.width = bnds.width + 4;
				Pen.translate( item, bottom );
				Pen.scale( *scaleAmt );
				Pen.font = Font( Font.defaultSansFace, 9 );
				Pen.color = Color.gray.alpha_(0.25);
				Pen.addRect( bnds.moveBy( 0, bnds.height.neg - 1 ) ).fill;
				Pen.color = Color.white.alpha_(0.5);
				Pen.stringAtPoint(
					(firstBar + (i * round)).asString,
					2@(bnds.height.neg - 1) 
				);
			});
		});
	}
}