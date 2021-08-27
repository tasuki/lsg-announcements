import ammonite.ops._

// data

val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"

case class Elimination(
  label: String,
  participants: Int,
  double: Boolean,
  rounds: Int,
  verticalSteps: Int,
  drops: Seq[Seq[Player]],
  extras: Seq[SvgContext => xml.Elem],
) {
  // symmetrical, sums to zero
  val nudges =
    (1 to (1 + rounds)).map(r => (2 + rounds - 2 * r) / 2)
}
case class Player(label: Option[String], round: Int, topOffset: Int)
case class Match(label: Option[String], p1: Player, p2: Player) {
  val winner: Player = Player(
    None,
    math.max(p1.round, p2.round) + 1,
    (p1.topOffset + p2.topOffset) / 2
  )
}

case class PlayOffSettings(print: (Int => Boolean), shift: (Int => Int))
val noLabelPOsettings = PlayOffSettings({_ => false}, {_ => 0})
val labelPOsettings = PlayOffSettings({_ => true}, {_ => 0})


// functions

def getPower(p: Int): Int =
  if (p > 2) 1 + getPower(p / 2)
  else 1

def seedPlayers(players: Seq[Int], step: Int, steps: Int): Seq[Int] =
  if (step > steps) players
  else {
    val magic = scala.math.pow(2, step).toInt + 1
    val newPlayers = players.flatMap(p => Seq(p, magic - p))
    seedPlayers(newPlayers, step + 1, steps)
  }

def playersMatch(players: Seq[Player], round: Int, printLabel: Boolean): Seq[Match] = {
  players.sortBy(_.topOffset).sliding(2, 2).zipWithIndex.flatMap {
    case (List(p1, p2), i) => {
      val label =
        if (printLabel) Some(round.toString + alphabet(i))
        else None
      Some(Match(label, p1, p2))
    }
    case _ => None
  }.toSeq
}

def playOff(
  round: Int,
  players: Seq[Player],
  drops: Seq[Seq[Player]],
  playOffSettings: PlayOffSettings,
): (Seq[Match], Seq[Seq[Player]]) = {
  val allPlayers = players ++ drops.headOption.getOrElse(Nil)
  val matches = playersMatch(allPlayers, round, playOffSettings.print(round))
  val winners = matches.map(_.winner)

  if (winners.length <= 1 && drops.flatten.length < 1) {
    // there's no more than one winner and there are no more drops
    if (winners.length == 0) (matches, Seq(allPlayers))
    else (matches, Seq(winners))
  } else {
    val (moreMatches, moreWinners) =
      playOff(round + 1, winners, drops.drop(1), playOffSettings)
    (moreMatches ++ matches, moreWinners.prepended(winners))
  }
}



// view

def loserLabelBottomRight(context: SvgContext): xml.Elem = {
  val (x, y) = (2350, 1300)
  <text y={s"$y"} style={context.bigStyle}>
    <tspan xml:space="preserve" x={s"$x"} dy="1.2em">drabinka</tspan>
    <tspan xml:space="preserve" x={s"$x"} dy="1.2em"> drugiej</tspan>
    <tspan xml:space="preserve" x={s"$x"} dy="1.2em"> nadziei</tspan>
  </text>
}

def loserLabelBottomLeft(context: SvgContext): xml.Elem = {
  val (x, y) = (100, 1500)
  <text x={s"$x"} y={s"$y"}
      transform={s"rotate(-90,$x,$y)"} style={context.bigStyle}>
    <tspan xml:space="preserve" x="0" dy="1.2em">drabinka</tspan>
    <tspan xml:space="preserve" x="0" dy="1.2em"> drugiej</tspan>
    <tspan xml:space="preserve" x="0" dy="1.2em"> nadziei</tspan>
  </text>
}

def placeMedal(place: Int, round: Int, topOffset: Int)(context: SvgContext): xml.Elem = {
  val (x, y) = context.getPlayerCoords(Player(None, round, topOffset))
  <g transform={s"translate(${x + context.playerWidth/2 - 25},${y})"}>
    <text text-anchor="middle" x="25" y="45" style={context.bigStyle}>{place.toString}</text>
    <circle r="35" cx="25" cy="25" style={context.pathStyle} />
    <path style={context.pathStyle} d="m  2,-4 l -15,-20 h  28 l  7,13" />
    <path style={context.pathStyle} d="m 48,-4 l  15,-20 h -28 l -7,13" />
  </g>
}

def dummyMatch(round: Int, from: Int, to: Int)(context: SvgContext): xml.Elem =
  context.getMatchPath(Match(
    None,
    Player(None, round, from),
    Player(None, round, to),
  ))

def dummyPlayer(round: Int, topOffset: Int)(context: SvgContext): xml.Elem =
  context.getPlayerPath(Player(None, round, topOffset))

case class SvgContext(height: Int, width: Int, padding: Int, e: Elimination) {
  val contentHeight = height - padding * 2
  val contentWidth = width - padding * 2 - 40
  val playerWidth = contentWidth / (e.rounds + 1)
  val fontStyle = "font-size: 40; font-family: Fira Mono; font-variant-numeric: lining-nums tabular-nums;"
  val bigStyle = "font-size: 60; font-family: Fira Mono;"
  val pathStyle = "stroke: black; stroke-width: 4; stroke-linecap: round; fill: none;"

  def getPlayerCoords(p: Player): (Int, Int) = {
    val x = 40 + padding + ((p.round - 1) * contentWidth) / (e.rounds + 1)
    val y = -40 + padding + (p.topOffset * contentHeight) / e.verticalSteps
    (x, y)
  }

  def getPlayerPath(p: Player): xml.Elem = {
    val (x, y) = getPlayerCoords(p)
    val text = p.label match {
      case Some(label) => <text x={(x - 4).toString} y={y.toString}
            text-anchor="end"
            style={fontStyle}>{label}</text>
      case None => xml.Text("")
    }

    <g>
      {text}
      <path d={s"M $x,$y h $playerWidth"} style={pathStyle}/>
    </g>
  }

  def getMatchPath(m: Match): xml.Elem = {
    val (x1, y1) = getPlayerCoords(m.p1)
    val (x2, y2) = getPlayerCoords(m.p2)
    val (mx, my) = getPlayerCoords(m.winner)
    val text = m.label match {
      case Some(label) => <text x={(mx - 10).toString} y={my.toString}
              dy=".35em" text-anchor="end" style={fontStyle}>{label}</text>
      case _ => xml.Text("")
    }
    val extend =
      if (x2 > x1) {
        <path d={s"M ${x1+playerWidth},${y1} h ${x2 - x1}"} style={pathStyle}/>
      } else xml.Text("")

    <g>
      {extend} {text}
      <path d={s"M ${x2+playerWidth},${y1} L ${x2+playerWidth},${y2}"} style={pathStyle}/>
    </g>
  }

  def getRoundPath(r: Int): xml.Elem = {
    val roundModifier =
      if (e.drops.nonEmpty && e.participants > 16) 1
      else 0

    val text = if (r == 1) "runda" else {s"${r + roundModifier}."}
    val (x, y) = getPlayerCoords(Player(None, r, e.verticalSteps))
    <text x={(x + playerWidth/2).toString} y={(y + 40).toString}
      text-anchor="middle" style={bigStyle}>{text}</text>
  }
}


def draw(e: Elimination): xml.Elem = {
  val initialPlayers: Seq[Player] = seedPlayers(Seq(1), 1, getPower(e.participants))
    .zipWithIndex.map {
      case (label, i) => Player(Some(label.toString), 1, 2+2*i)
    }

  def getDoublePlayersAndMatches() = {
    if (e.drops.nonEmpty && e.participants > 16) {
      // loser bracket only
      val (losermatches, losers) = playOff(2, Nil, e.drops, noLabelPOsettings)
      (e.drops.flatten ++ losers.flatten,
      losermatches)
    } else {
      // winner & loser bracket (loser bracket empty if no drops)
      val (winnermatches, winners) = playOff(1, initialPlayers, Nil, labelPOsettings)
      val (losermatches, losers) = playOff(2, Nil, e.drops, noLabelPOsettings)

      val finale: Seq[Match] = playersMatch(winners.last ++ losers.last, e.rounds, false)
      val winner: Seq[Player] = finale.map(_.winner)

      (initialPlayers ++ winners.flatten ++ e.drops.flatten ++ losers.flatten ++ winner,
      winnermatches ++ losermatches ++ finale)
    }
  }

  def getSinglePlayersAndMatches() = {
    val (winnermatches, winners) = playOff(1, initialPlayers, Nil, noLabelPOsettings)
    (initialPlayers ++ winners.flatten,
    winnermatches)
  }

  val (players: Seq[Player], matches: Seq[Match]) =
    if (e.double) getDoublePlayersAndMatches()
    else getSinglePlayersAndMatches()

  val context = SvgContext(2100, 2970, 50, e)

  <svg
      xmlns="http://www.w3.org/2000/svg"
      xmlns:xlink="http://www.w3.org/1999/xlink"
      viewBox={s"0 0 ${context.width} ${context.height}"}
      width="297mm" height="210mm">
    <rect fill="white" x="0" y="0"
      width={context.width.toString} height={context.height.toString} />
    { players.map(context.getPlayerPath) }
    { matches.map(context.getMatchPath) }
    { (1 to e.rounds).map(context.getRoundPath) }
    { e.extras.map(extra => extra(context)) }
  </svg>
}

val drops8 = Seq(
  Seq(
    Player(Some("1A"), 2, 20),
    Player(Some("1B"), 2, 22),
    Player(Some("1C"), 2, 26),
    Player(Some("1D"), 2, 28),
  ),
  Seq(
    Player(Some("2B"), 3, 19),
    Player(Some("2A"), 3, 25),
  ),
  Seq(),
  Seq(
    Player(Some("3A"), 5, 18),
  ),
)

val drops16 = Seq(
  Seq(
    Player(Some("1A"), 2, 34),
    Player(Some("1B"), 2, 36),
    Player(Some("1E"), 2, 39),
    Player(Some("1F"), 2, 41),
    Player(Some("1C"), 2, 44),
    Player(Some("1D"), 2, 46),
    Player(Some("1G"), 2, 49),
    Player(Some("1H"), 2, 51),
  ),
  Seq(
    Player(Some("2B"), 3, 33),
    Player(Some("2D"), 3, 38),
    Player(Some("2A"), 3, 43),
    Player(Some("2C"), 3, 48),
  ),
  Seq(
    Player(Some("3B"), 4, 32),
    Player(Some("3A"), 4, 37),
  ),
  Seq(
    Player(Some("4A"), 5, 42),
  ),
)

val drops32 = Seq(
  Seq(
    Player(Some("1A"), 1,  3),
    Player(Some("1B"), 1,  5),
    Player(Some("1C"), 1,  8),
    Player(Some("1D"), 1, 10),
    Player(Some("1I"), 1, 13),
    Player(Some("1J"), 1, 15),
    Player(Some("1K"), 1, 18),
    Player(Some("1L"), 1, 20),
    Player(Some("1E"), 1, 23),
    Player(Some("1F"), 1, 25),
    Player(Some("1G"), 1, 28),
    Player(Some("1H"), 1, 30),
    Player(Some("1M"), 1, 33),
    Player(Some("1N"), 1, 35),
    Player(Some("1O"), 1, 38),
    Player(Some("1P"), 1, 40),
  ),
  Seq(
    Player(Some("2C"), 2,  2),
    Player(Some("2D"), 2,  7),
    Player(Some("2G"), 2, 12),
    Player(Some("2H"), 2, 17),
    Player(Some("2E"), 2, 22),
    Player(Some("2F"), 2, 27),
    Player(Some("2A"), 2, 32),
    Player(Some("2B"), 2, 37),
  ),
  Seq(),
  Seq(
    Player(Some("3C"), 4,  2),
    Player(Some("3A"), 4, 12),
    Player(Some("3D"), 4, 22),
    Player(Some("3B"), 4, 32),
  ),
  Seq(
    Player(Some("4A"), 5,  8),
    Player(Some("4B"), 5, 18),
  ),
  Seq(
    Player(Some("5A"), 6, 36),
  ),
)

Seq(
  Elimination("single-elim-08.svg",  8, false, 3, 18, Nil, Nil),
  Elimination("single-elim-16.svg", 16, false, 4, 34, Nil, Nil),
  Elimination("single-elim-32.svg", 32, false, 5, 66, Nil, Nil),

  Elimination("double-elim-08.svg",  8,  true, 6,  30, drops8,  Seq(
    loserLabelBottomLeft,
    placeMedal(1, 7, 15),
    placeMedal(2, 6, 15),
    placeMedal(3, 5, 15),
    placeMedal(4, 4, 15),
  )),
  Elimination("double-elim-16.svg", 16,  true, 7,  52, drops16, Seq(
    loserLabelBottomLeft,
    placeMedal(1, 8, 29),
    placeMedal(2, 7, 29),
    placeMedal(3, 6, 29),
    placeMedal(4, 5, 29),
  )),

  Elimination("double-elim-32-1.svg", 32, true, 6, 66, Nil, Seq(
    dummyMatch(6, 33, 64),
    dummyPlayer(7, 60),
    placeMedal(1, 7, 50),
    placeMedal(2, 6, 50),
  )),
  Elimination("double-elim-32-2.svg", 32, true, 8, 42, drops32, Seq(
    dummyMatch(8, 21, 2),
    loserLabelBottomRight,
    placeMedal(2, 8, 22),
    placeMedal(3, 7, 22),
    placeMedal(4, 6, 22),
  )),
).map { e =>
  val pp = new xml.PrettyPrinter(100, 2)
  write.over(pwd/"drabinki-pdf"/e.label, pp.format(draw(e)))
}
